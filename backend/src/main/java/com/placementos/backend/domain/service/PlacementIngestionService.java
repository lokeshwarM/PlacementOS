package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.ExtractedRoleDto;
import com.placementos.backend.domain.dto.StructuredPlacementExtractionResult;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.enums.DriveStatus;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.PlacementRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for validating and ingesting structured placement extraction results
 * submitted by the Python processing service into PostgreSQL business state.
 *
 * Enforces:
 * 1. Strict validation of required fields and confidence thresholds.
 * 2. Safe handling of non-placement emails without creating partial business records.
 * 3. Idempotent and deterministic PlacementDrive + PlacementRole persistence on retries.
 * 4. Clear separation of common drive eligibility from role-specific eligibility.
 */
@Service
public class PlacementIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PlacementIngestionService.class);

    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final ProcessedEmailService processedEmailService;

    public PlacementIngestionService(PlacementDriveRepository placementDriveRepository,
                                     PlacementRoleRepository placementRoleRepository,
                                     ProcessedEmailService processedEmailService) {
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.processedEmailService = processedEmailService;
    }

    /**
     * Ingests a structured extraction result.
     *
     * @param result The structured extraction output from the Python processing service.
     * @return Optional containing the persisted {@link PlacementDrive}, or empty if non-placement or invalid.
     */
    @Transactional
    public Optional<PlacementDrive> ingestExtractionResult(StructuredPlacementExtractionResult result) {
        if (result == null || result.getMessageId() == null || result.getMessageId().isBlank()) {
            log.warn("Rejected null or empty extraction result");
            return Optional.empty();
        }

        String messageId = result.getMessageId();

        // 1. Check classification
        if (result.getClassification() == null) {
            log.warn("Extraction result for message {} lacks classification", messageId);
            processedEmailService.markFailed(messageId, "Missing classification in extraction result");
            return Optional.empty();
        }

        if (Boolean.FALSE.equals(result.getClassification().getIsPlacement())) {
            String reason = result.getClassification().getEvidence() != null && !result.getClassification().getEvidence().isEmpty()
                    ? String.join(", ", result.getClassification().getEvidence())
                    : "Classified as non-placement";
            log.info("Message {} classified as NON_PLACEMENT. Skipping drive creation.", messageId);
            processedEmailService.markNonPlacement(messageId, reason);
            return Optional.empty();
        }

        // 2. Validate placement extraction requirements
        if (result.getCompanyName() == null || result.getCompanyName().isBlank()) {
            log.warn("Placement extraction for message {} is missing company name. Rejecting partial record.", messageId);
            processedEmailService.markFailed(messageId, "Validation failed: missing company name in placement extraction");
            return Optional.empty();
        }

        Double confidence = result.getClassification().getConfidence();
        if (confidence != null && confidence < 0.50) {
            log.warn("Placement extraction for message {} has low confidence ({}). Rejecting automatic creation.", messageId, confidence);
            processedEmailService.markFailed(messageId, "Low confidence classification: " + confidence);
            return Optional.empty();
        }

        // 3. Idempotently create or update PlacementDrive
        PlacementDrive drive = placementDriveRepository.findBySourceEmailId(messageId)
                .orElseGet(() -> {
                    PlacementDrive newDrive = new PlacementDrive();
                    newDrive.setSourceEmailId(messageId);
                    newDrive.setStatus(DriveStatus.OPEN);
                    return newDrive;
                });

        drive.setCompanyName(result.getCompanyName().trim());
        drive.setTitle(result.getDriveTitle() != null ? result.getDriveTitle().trim() : result.getCompanyName().trim());
        drive.setDescription(result.getDescription());
        drive.setApplicationDeadline(result.getApplicationDeadline());

        if (result.getCommonEligibility() != null) {
            drive.setEligibilityCriteria(result.getCommonEligibility().toMap());
        }

        PlacementDrive savedDrive = placementDriveRepository.saveAndFlush(drive);

        // 4. Synchronize PlacementRoles deterministically
        synchronizeRoles(savedDrive, result.getRoles());

        // 5. Update processing state
        processedEmailService.markExtracted(messageId);

        log.info("Successfully ingested placement drive {} (id={}) for message {} with {} roles.",
                savedDrive.getCompanyName(), savedDrive.getId(), messageId, savedDrive.getRoles().size());

        return Optional.of(savedDrive);
    }

    /**
     * Reconciles existing roles with the newly extracted roles deterministically,
     * avoiding unnecessary database churn while accurately applying updates.
     */
    private void synchronizeRoles(PlacementDrive drive, List<ExtractedRoleDto> extractedRoles) {
        if (extractedRoles == null || extractedRoles.isEmpty()) {
            // Default single fallback role if no specific role titles were extracted
            extractedRoles = List.of(new ExtractedRoleDto(drive.getTitle() != null ? drive.getTitle() : "General", 1));
        }

        List<PlacementRole> existingRoles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(drive.getId());
        Map<String, PlacementRole> existingByNormalizedTitle = existingRoles.stream()
                .collect(Collectors.toMap(
                        r -> r.getRoleTitle().trim().toLowerCase(),
                        r -> r,
                        (r1, r2) -> r1
                ));

        Set<String> incomingNormalizedTitles = new HashSet<>();
        int order = 1;

        for (ExtractedRoleDto extracted : extractedRoles) {
            String title = extracted.getTitle() != null && !extracted.getTitle().isBlank()
                    ? extracted.getTitle().trim()
                    : "Role " + order;
            String normalizedTitle = title.toLowerCase();
            incomingNormalizedTitles.add(normalizedTitle);

            PlacementRole role = existingByNormalizedTitle.get(normalizedTitle);
            if (role == null) {
                role = new PlacementRole();
                role.setPlacementDrive(drive);
                role.setRoleTitle(title);
                drive.addRole(role);
            }

            role.setRoleDescription(extracted.getDescription());
            role.setRoleOrder(extracted.getRoleOrder() != null ? extracted.getRoleOrder() : order);
            if (extracted.getEligibility() != null) {
                role.setEligibilityCriteria(extracted.getEligibility().toMap());
            } else {
                role.setEligibilityCriteria(null);
            }

            placementRoleRepository.save(role);
            order++;
        }

        // Remove obsolete roles that are no longer present in the updated extraction
        for (PlacementRole existing : existingRoles) {
            if (!incomingNormalizedTitles.contains(existing.getRoleTitle().trim().toLowerCase())) {
                drive.removeRole(existing);
                placementRoleRepository.delete(existing);
            }
        }
    }
}

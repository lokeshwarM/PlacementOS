package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.DocumentProcessingResultDto;
import com.placementos.backend.domain.dto.ShortlistCandidateDto;
import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.normalizer.NameNormalizer;
import com.placementos.backend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Business service responsible for deterministic student matching and shortlist entry persistence.
 * Implements strict identifier hierarchy (Registration Number -> NeoPAT ID -> Unique Exact Normalized Name).
 */
@Service
@Transactional(readOnly = true)
public class ShortlistMatchingService {

    private static final Logger log = LoggerFactory.getLogger(ShortlistMatchingService.class);

    private final ShortlistEntryRepository shortlistEntryRepository;
    private final StudentRepository studentRepository;
    private final AttachmentRepository attachmentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;

    public ShortlistMatchingService(ShortlistEntryRepository shortlistEntryRepository,
                                  StudentRepository studentRepository,
                                  AttachmentRepository attachmentRepository,
                                  PlacementDriveRepository placementDriveRepository,
                                  PlacementRoleRepository placementRoleRepository) {
        this.shortlistEntryRepository = shortlistEntryRepository;
        this.studentRepository = studentRepository;
        this.attachmentRepository = attachmentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
    }

    /**
     * Ingests structured document processing results, matches candidates against students,
     * and idempotently persists ShortlistEntry records.
     */
    @Transactional
    public List<ShortlistEntry> processDocumentShortlist(DocumentProcessingResultDto resultDto) {
        Objects.requireNonNull(resultDto, "resultDto must not be null");

        Attachment attachment = null;
        if (resultDto.getAttachmentId() != null) {
            attachment = attachmentRepository.findById(resultDto.getAttachmentId())
                    .orElse(null);
        }

        // Handle OCR_REQUIRED documents
        if (Boolean.TRUE.equals(resultDto.getOcrRequired())) {
            if (attachment != null) {
                attachment.setParsedStatus(AttachmentParsedStatus.OCR_REQUIRED);
                attachmentRepository.save(attachment);
                log.info("Attachment id={} marked as OCR_REQUIRED.", attachment.getId());
            }
            return Collections.emptyList();
        }

        // Handle non-shortlist documents
        String classification = resultDto.getClassification();
        if (classification != null && !"SHORTLIST".equalsIgnoreCase(classification)) {
            if (attachment != null) {
                attachment.setParsedStatus(AttachmentParsedStatus.PARSED);
                attachmentRepository.save(attachment);
                log.info("Attachment id={} classified as {}. Skipping shortlist candidate matching.",
                        attachment.getId(), classification);
            }
            return Collections.emptyList();
        }

        // Resolve PlacementDrive
        PlacementDrive drive = resolvePlacementDrive(attachment);
        if (drive == null) {
            log.warn("Cannot process shortlist: No PlacementDrive associated with attachment id={}",
                    resultDto.getAttachmentId());
            if (attachment != null) {
                attachment.setParsedStatus(AttachmentParsedStatus.REVIEW_REQUIRED);
                attachmentRepository.save(attachment);
            }
            return Collections.emptyList();
        }

        List<PlacementRole> driveRoles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(drive.getId());
        List<ShortlistEntry> persistedEntries = new ArrayList<>();

        for (ShortlistCandidateDto candidate : resultDto.getCandidates()) {
            ShortlistEntry entry = matchAndPersistCandidate(drive, attachment, driveRoles, candidate, resultDto.getConfidence());
            persistedEntries.add(entry);
        }

        if (attachment != null) {
            attachment.setPlacementDrive(drive);
            attachment.setParsedStatus(AttachmentParsedStatus.EXTRACTED);
            attachmentRepository.save(attachment);
        }

        log.info("Processed {} shortlist candidates for drive id={} from attachment id={}",
                persistedEntries.size(), drive.getId(), resultDto.getAttachmentId());

        return persistedEntries;
    }

    private ShortlistEntry matchAndPersistCandidate(PlacementDrive drive,
                                                    Attachment attachment,
                                                    List<PlacementRole> driveRoles,
                                                    ShortlistCandidateDto candidate,
                                                    Double documentConfidence) {
        String cleanRegNo = sanitizeIdentifier(candidate.getRegistrationNumber());
        String cleanNeopat = sanitizeIdentifier(candidate.getNeopatId());
        String rawName = candidate.getName() != null ? candidate.getName().trim() : null;

        // 1. Resolve role if candidate has role specified
        PlacementRole matchedRole = resolveRole(driveRoles, candidate.getRole());

        // 2. Deterministic Student Matching
        MatchResolution resolution = resolveStudent(cleanRegNo, cleanNeopat, rawName);

        // 3. Find or Create ShortlistEntry for Idempotency
        ShortlistEntry entry = findExistingEntry(drive.getId(), attachment != null ? attachment.getId() : null, cleanRegNo, cleanNeopat, rawName)
                .orElseGet(ShortlistEntry::new);

        entry.setPlacementDrive(drive);
        entry.setSourceAttachment(attachment);
        entry.setPlacementRole(matchedRole);
        entry.setRegistrationNumber(cleanRegNo);
        entry.setNeopatId(cleanNeopat);
        entry.setCandidateName(rawName);
        entry.setStudent(resolution.student);
        entry.setMatchStatus(resolution.status);
        entry.setMatchMethod(resolution.method);
        entry.setMatchReason(resolution.reason);
        entry.setRawEvidence(candidate.getEvidence());

        if (documentConfidence != null) {
            entry.setConfidence(BigDecimal.valueOf(documentConfidence));
        } else if (resolution.status == ShortlistMatchStatus.MATCHED) {
            entry.setConfidence(BigDecimal.ONE);
        }

        return shortlistEntryRepository.save(entry);
    }

    private MatchResolution resolveStudent(String regNo, String neopatId, String candidateName) {
        Student studentByReg = null;
        if (regNo != null && !regNo.isBlank()) {
            studentByReg = studentRepository.findByRegistrationNumber(regNo).orElse(null);
        }

        Student studentByNeo = null;
        if (neopatId != null && !neopatId.isBlank()) {
            studentByNeo = studentRepository.findByNeopatId(neopatId).orElse(null);
        }

        // Check for strong-identifier conflict
        if (studentByReg != null && studentByNeo != null) {
            if (!studentByReg.getId().equals(studentByNeo.getId())) {
                return new MatchResolution(
                        null,
                        ShortlistMatchStatus.AMBIGUOUS,
                        "STRONG_IDENTIFIER_CONFLICT",
                        String.format("Conflicting identifiers: Registration number %s belongs to student '%s' (id=%d) but NeoPAT ID %s belongs to student '%s' (id=%d).",
                                regNo, studentByReg.getName(), studentByReg.getId(),
                                neopatId, studentByNeo.getName(), studentByNeo.getId())
                );
            }
            return new MatchResolution(
                    studentByReg,
                    ShortlistMatchStatus.MATCHED,
                    "REGISTRATION_NUMBER_AND_NEOPAT",
                    String.format("Matched by registration number %s and confirmed by NeoPAT ID %s.", regNo, neopatId)
            );
        }

        // Match by Registration Number alone
        if (studentByReg != null) {
            return new MatchResolution(
                    studentByReg,
                    ShortlistMatchStatus.MATCHED,
                    "REGISTRATION_NUMBER",
                    String.format("Matched by registration number %s.", regNo)
            );
        }

        // Match by NeoPAT ID alone
        if (studentByNeo != null) {
            return new MatchResolution(
                    studentByNeo,
                    ShortlistMatchStatus.MATCHED,
                    "NEOPAT_ID",
                    String.format("Matched by NeoPAT ID %s.", neopatId)
            );
        }

        // If strong identifiers were supplied but neither was found in DB
        if ((regNo != null && !regNo.isBlank()) || (neopatId != null && !neopatId.isBlank())) {
            // An unknown strong identifier must NOT silently match by name to an existing student
            return new MatchResolution(
                    null,
                    ShortlistMatchStatus.UNMATCHED,
                    "UNRECOGNIZED_STRONG_IDENTIFIER",
                    "No registered student found matching provided registration number or NeoPAT ID."
            );
        }

        // Fall back to Exact Normalized Name Match only if NO strong identifiers were present
        if (candidateName != null && !candidateName.isBlank()) {
            String normalized = NameNormalizer.normalize(candidateName);
            if (!normalized.isEmpty()) {
                List<Student> allStudents = studentRepository.findAll();
                List<Student> matchingStudents = new ArrayList<>();
                for (Student s : allStudents) {
                    if (NameNormalizer.matches(candidateName, s.getName())) {
                        matchingStudents.add(s);
                    }
                }

                if (matchingStudents.size() == 1) {
                    Student s = matchingStudents.get(0);
                    return new MatchResolution(
                            s,
                            ShortlistMatchStatus.MATCHED,
                            "EXACT_NORMALIZED_NAME",
                            String.format("Matched uniquely by exact normalized name '%s'.", candidateName)
                    );
                } else if (matchingStudents.size() > 1) {
                    return new MatchResolution(
                            null,
                            ShortlistMatchStatus.AMBIGUOUS,
                            "AMBIGUOUS_NAME",
                            String.format("Found %d students with exact normalized name '%s'. Manual review required.",
                                    matchingStudents.size(), candidateName)
                    );
                }
            }
        }

        return new MatchResolution(
                null,
                ShortlistMatchStatus.UNMATCHED,
                "NO_MATCH",
                "No registered student found matching candidate information."
        );
    }

    private Optional<ShortlistEntry> findExistingEntry(Long driveId, Long attachmentId, String regNo, String neopatId, String name) {
        if (attachmentId == null) return Optional.empty();

        if (regNo != null && !regNo.isBlank()) {
            return shortlistEntryRepository.findByPlacementDriveIdAndSourceAttachmentIdAndRegistrationNumber(driveId, attachmentId, regNo);
        }
        if (neopatId != null && !neopatId.isBlank()) {
            return shortlistEntryRepository.findByPlacementDriveIdAndSourceAttachmentIdAndNeopatId(driveId, attachmentId, neopatId);
        }
        if (name != null && !name.isBlank()) {
            return shortlistEntryRepository.findByPlacementDriveIdAndSourceAttachmentIdAndCandidateName(driveId, attachmentId, name);
        }
        return Optional.empty();
    }

    private PlacementDrive resolvePlacementDrive(Attachment attachment) {
        if (attachment == null) return null;
        if (attachment.getPlacementDrive() != null) {
            return attachment.getPlacementDrive();
        }
        if (attachment.getGmailMessage() != null) {
            String messageId = attachment.getGmailMessage().getMessageId();
            return placementDriveRepository.findBySourceEmailId(messageId).orElse(null);
        }
        return null;
    }

    private PlacementRole resolveRole(List<PlacementRole> roles, String roleName) {
        if (roleName == null || roleName.isBlank() || roles == null) {
            return null;
        }
        String clean = roleName.trim();
        for (PlacementRole r : roles) {
            if (r.getRoleTitle().equalsIgnoreCase(clean)) {
                return r;
            }
        }
        return null;
    }

    private String sanitizeIdentifier(String id) {
        if (id == null || id.isBlank()) return null;
        return id.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static class MatchResolution {
        final Student student;
        final ShortlistMatchStatus status;
        final String method;
        final String reason;

        MatchResolution(Student student, ShortlistMatchStatus status, String method, String reason) {
            this.student = student;
            this.status = status;
            this.method = method;
            this.reason = reason;
        }
    }
}

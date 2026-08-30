package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.AttachmentRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.ShortlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business service for shortlist entry persistence operations.
 *
 * This service manages only the database records produced by shortlist parsing.
 * No document parsing, OCR, or fuzzy matching is implemented here.
 * The Python processing service creates entry data; Spring Boot persists it.
 *
 * A shortlist entry may identify a candidate by any combination of:
 *  - registration_number
 *  - neopat_id
 *  - candidate_name
 * At least one must be non-null (enforced at the application layer).
 */
@Service
@Transactional(readOnly = true)
public class ShortlistService {

    private final ShortlistEntryRepository shortlistEntryRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final AttachmentRepository attachmentRepository;

    public ShortlistService(ShortlistEntryRepository shortlistEntryRepository,
                            PlacementDriveRepository placementDriveRepository,
                            AttachmentRepository attachmentRepository) {
        this.shortlistEntryRepository = shortlistEntryRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.attachmentRepository = attachmentRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Persists a shortlist entry.
     * At least one of registrationNumber, neopatId, or candidateName must be non-null.
     *
     * @param sourceAttachmentId nullable — the attachment this entry was extracted from.
     */
    @Transactional
    public ShortlistEntry createEntry(Long placementDriveId,
                                      Long sourceAttachmentId,
                                      String registrationNumber,
                                      String neopatId,
                                      String candidateName,
                                      String matchMethod,
                                      BigDecimal confidence) {
        if (registrationNumber == null && neopatId == null && candidateName == null) {
            throw new IllegalArgumentException(
                    "At least one of registrationNumber, neopatId, or candidateName must be provided.");
        }

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        Attachment sourceAttachment = null;
        if (sourceAttachmentId != null) {
            sourceAttachment = attachmentRepository.findById(sourceAttachmentId)
                    .orElseThrow(() -> ResourceNotFoundException.attachment(sourceAttachmentId));
        }

        ShortlistEntry entry = new ShortlistEntry();
        entry.setPlacementDrive(drive);
        entry.setSourceAttachment(sourceAttachment);
        entry.setRegistrationNumber(registrationNumber);
        entry.setNeopatId(neopatId);
        entry.setCandidateName(candidateName);
        entry.setMatchMethod(matchMethod);
        entry.setConfidence(confidence);

        return shortlistEntryRepository.save(entry);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public List<ShortlistEntry> findByPlacementDriveId(Long placementDriveId) {
        return shortlistEntryRepository.findByPlacementDriveId(placementDriveId);
    }

    public List<ShortlistEntry> findByRegistrationNumber(String registrationNumber) {
        return shortlistEntryRepository.findByRegistrationNumber(registrationNumber);
    }

    public List<ShortlistEntry> findByNeopatId(String neopatId) {
        return shortlistEntryRepository.findByNeopatId(neopatId);
    }

    public List<ShortlistEntry> findByCandidateName(String candidateName) {
        return shortlistEntryRepository.findByCandidateNameContainingIgnoreCase(candidateName);
    }
}

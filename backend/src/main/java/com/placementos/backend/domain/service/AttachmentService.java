package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.AttachmentRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business service for attachment metadata operations.
 *
 * Binary file content is NOT stored in PostgreSQL.
 * This service manages only metadata (filename, content type, storage reference, parse status).
 * The actual file storage mechanism (object store, file path) is managed by the
 * Python processing service and referenced here via storage_reference.
 *
 * No file upload, download, or parsing logic is implemented here.
 */
@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             PlacementDriveRepository placementDriveRepository) {
        this.attachmentRepository = attachmentRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    /**
     * Registers a new attachment record for a placement drive.
     * The storage_reference should be populated by the processing service after the
     * file has been stored externally.
     */
    @Transactional
    public Attachment createAttachment(Long placementDriveId,
                                       String filename,
                                       String contentType,
                                       String storageReference) {
        PlacementDrive drive = placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(placementDriveId));

        Attachment attachment = new Attachment();
        attachment.setPlacementDrive(drive);
        attachment.setFilename(filename);
        attachment.setContentType(contentType);
        attachment.setStorageReference(storageReference);
        attachment.setParsedStatus(AttachmentParsedStatus.PENDING);

        return attachmentRepository.save(attachment);
    }

    /**
     * Updates the parsed status of an attachment — called by the processing pipeline
     * after the Python service has attempted to parse the file.
     */
    @Transactional
    public Attachment updateParsedStatus(Long attachmentId, AttachmentParsedStatus newStatus) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> ResourceNotFoundException.attachment(attachmentId));
        attachment.setParsedStatus(newStatus);
        return attachmentRepository.save(attachment);
    }

    public List<Attachment> findByPlacementDriveId(Long placementDriveId) {
        return attachmentRepository.findByPlacementDriveId(placementDriveId);
    }

    public List<Attachment> findPending() {
        return attachmentRepository.findByParsedStatus(AttachmentParsedStatus.PENDING);
    }
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistence repository for {@link Attachment}.
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByPlacementDriveId(Long placementDriveId);

    List<Attachment> findByGmailMessageId(Long gmailMessageId);

    List<Attachment> findByParsedStatus(AttachmentParsedStatus parsedStatus);
}

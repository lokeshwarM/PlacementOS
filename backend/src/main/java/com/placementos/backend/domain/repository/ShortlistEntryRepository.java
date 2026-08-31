package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link ShortlistEntry}.
 * Supports lookup by identifiers, status, attachment, drive, and student.
 */
public interface ShortlistEntryRepository extends JpaRepository<ShortlistEntry, Long> {

    List<ShortlistEntry> findByPlacementDriveId(Long placementDriveId);

    List<ShortlistEntry> findBySourceAttachmentId(Long sourceAttachmentId);

    List<ShortlistEntry> findByStudentId(Long studentId);

    List<ShortlistEntry> findByMatchStatus(ShortlistMatchStatus matchStatus);

    List<ShortlistEntry> findByRegistrationNumber(String registrationNumber);

    List<ShortlistEntry> findByNeopatId(String neopatId);

    List<ShortlistEntry> findByCandidateNameContainingIgnoreCase(String candidateName);

    Optional<ShortlistEntry> findByPlacementDriveIdAndSourceAttachmentIdAndRegistrationNumber(
            Long placementDriveId, Long sourceAttachmentId, String registrationNumber);

    Optional<ShortlistEntry> findByPlacementDriveIdAndSourceAttachmentIdAndNeopatId(
            Long placementDriveId, Long sourceAttachmentId, String neopatId);

    Optional<ShortlistEntry> findByPlacementDriveIdAndSourceAttachmentIdAndCandidateName(
            Long placementDriveId, Long sourceAttachmentId, String candidateName);
}

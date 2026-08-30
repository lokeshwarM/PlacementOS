package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.ShortlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistence repository for {@link ShortlistEntry}.
 * Supports lookup by all three candidate identifier types.
 */
public interface ShortlistEntryRepository extends JpaRepository<ShortlistEntry, Long> {

    List<ShortlistEntry> findByPlacementDriveId(Long placementDriveId);

    List<ShortlistEntry> findByRegistrationNumber(String registrationNumber);

    List<ShortlistEntry> findByNeopatId(String neopatId);

    List<ShortlistEntry> findByCandidateNameContainingIgnoreCase(String candidateName);
}

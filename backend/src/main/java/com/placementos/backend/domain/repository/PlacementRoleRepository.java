package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.PlacementRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link PlacementRole}.
 */
public interface PlacementRoleRepository extends JpaRepository<PlacementRole, Long> {

    List<PlacementRole> findByPlacementDriveIdOrderByRoleOrderAsc(Long placementDriveId);

    Optional<PlacementRole> findByPlacementDriveIdAndRoleTitle(Long placementDriveId, String roleTitle);
}

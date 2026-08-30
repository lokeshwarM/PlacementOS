package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.enums.DriveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Persistence repository for {@link PlacementDrive}.
 */
public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {

    List<PlacementDrive> findByStatus(DriveStatus status);

    List<PlacementDrive> findByCompanyNameContainingIgnoreCase(String companyName);
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence repository for {@link Application}.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    boolean existsByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    List<Application> findByStudentId(Long studentId);

    List<Application> findByPlacementDriveId(Long placementDriveId);

    List<Application> findByPlacementRoleId(Long placementRoleId);

    List<Application> findByStatus(ApplicationStatus status);
}

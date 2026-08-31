package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.StudentEligibilityResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEligibilityResultRepository extends JpaRepository<StudentEligibilityResult, Long> {

    Optional<StudentEligibilityResult> findByStudentIdAndPlacementRoleId(Long studentId, Long placementRoleId);

    List<StudentEligibilityResult> findByStudentIdAndPlacementDriveId(Long studentId, Long placementDriveId);

    List<StudentEligibilityResult> findByPlacementDriveId(Long placementDriveId);

    List<StudentEligibilityResult> findByStudentId(Long studentId);
}

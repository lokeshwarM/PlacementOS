package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.CriterionEvaluationResult;
import com.placementos.backend.domain.dto.DriveEligibilityEvaluationResponse;
import com.placementos.backend.domain.dto.RoleEligibilityEvaluationResponse;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.StudentEligibilityResult;
import com.placementos.backend.domain.enums.CriterionEvaluationStatus;
import com.placementos.backend.domain.enums.CriterionScope;
import com.placementos.backend.domain.enums.CriterionType;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.PlacementRoleRepository;
import com.placementos.backend.domain.repository.StudentEligibilityResultRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import com.placementos.backend.domain.service.evaluator.EligibilityEvaluationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EligibilityServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private StudentEligibilityResultRepository eligibilityResultRepository;

    @Mock
    private EligibilityEvaluationEngine evaluationEngine;

    private EligibilityService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityService(
                studentRepository,
                placementDriveRepository,
                placementRoleRepository,
                eligibilityResultRepository,
                evaluationEngine
        );
    }

    @Test
    void evaluateAndPersistForDrive_evaluatesAllRolesAndPersistsIdempotently() {
        Student student = new Student();
        student.setId(10L);
        student.setRegistrationNumber("21BCE0001");
        student.setName("Alice");

        PlacementDrive drive = new PlacementDrive();
        drive.setId(50L);
        drive.setCompanyName("Google");
        drive.setTitle("Google Campus Hiring");

        PlacementRole role1 = new PlacementRole("Software Engineer", 1);
        role1.setId(101L);
        role1.setPlacementDrive(drive);

        PlacementRole role2 = new PlacementRole("Data Scientist", 2);
        role2.setId(102L);
        role2.setPlacementDrive(drive);

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(placementDriveRepository.findById(50L)).thenReturn(Optional.of(drive));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(50L)).thenReturn(List.of(role1, role2));

        RoleEligibilityEvaluationResponse eval1 = new RoleEligibilityEvaluationResponse();
        eval1.setStudentId(10L);
        eval1.setPlacementDriveId(50L);
        eval1.setPlacementRoleId(101L);
        eval1.setRoleTitle("Software Engineer");
        eval1.setDecision(EligibilityDecision.ELIGIBLE);
        eval1.setEvaluatorVersion("eligibility-v1");
        eval1.setEvaluatedAt(Instant.now());
        eval1.setCriteriaResults(List.of(
                new CriterionEvaluationResult(CriterionType.MINIMUM_CGPA, CriterionEvaluationStatus.PASS, CriterionScope.DRIVE_COMMON, 7.5, new BigDecimal("8.5"), "Pass")
        ));

        RoleEligibilityEvaluationResponse eval2 = new RoleEligibilityEvaluationResponse();
        eval2.setStudentId(10L);
        eval2.setPlacementDriveId(50L);
        eval2.setPlacementRoleId(102L);
        eval2.setRoleTitle("Data Scientist");
        eval2.setDecision(EligibilityDecision.REVIEW_REQUIRED);
        eval2.setEvaluatorVersion("eligibility-v1");
        eval2.setEvaluatedAt(Instant.now());

        when(evaluationEngine.evaluate(student, drive, role1)).thenReturn(eval1);
        when(evaluationEngine.evaluate(student, drive, role2)).thenReturn(eval2);

        when(eligibilityResultRepository.findByStudentIdAndPlacementRoleId(10L, 101L)).thenReturn(Optional.empty());
        when(eligibilityResultRepository.findByStudentIdAndPlacementRoleId(10L, 102L)).thenReturn(Optional.empty());

        DriveEligibilityEvaluationResponse response = service.evaluateAndPersistForDrive(10L, 50L);

        assertNotNull(response);
        assertEquals(10L, response.getStudentId());
        assertEquals(50L, response.getPlacementDriveId());
        assertEquals(2, response.getRoleResults().size());
        assertEquals(EligibilityDecision.ELIGIBLE, response.getRoleResults().get(0).getDecision());
        assertEquals(EligibilityDecision.REVIEW_REQUIRED, response.getRoleResults().get(1).getDecision());

        // Verify saved to repository twice (once per role)
        verify(eligibilityResultRepository, times(2)).save(any(StudentEligibilityResult.class));
    }

    @Test
    void evaluateAndPersistForRole_updatesExistingResultSafely() {
        Student student = new Student();
        student.setId(10L);

        PlacementDrive drive = new PlacementDrive();
        drive.setId(50L);

        PlacementRole role = new PlacementRole("Software Engineer", 1);
        role.setId(101L);
        role.setPlacementDrive(drive);

        StudentEligibilityResult existingResult = new StudentEligibilityResult();
        existingResult.setId(999L);
        existingResult.setStudent(student);
        existingResult.setPlacementDrive(drive);
        existingResult.setPlacementRole(role);
        existingResult.setDecision(EligibilityDecision.NOT_ELIGIBLE);

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(placementRoleRepository.findById(101L)).thenReturn(Optional.of(role));
        when(eligibilityResultRepository.findByStudentIdAndPlacementRoleId(10L, 101L)).thenReturn(Optional.of(existingResult));

        RoleEligibilityEvaluationResponse eval = new RoleEligibilityEvaluationResponse();
        eval.setStudentId(10L);
        eval.setPlacementDriveId(50L);
        eval.setPlacementRoleId(101L);
        eval.setDecision(EligibilityDecision.ELIGIBLE);
        eval.setEvaluatorVersion("eligibility-v1");
        eval.setEvaluatedAt(Instant.now());

        when(evaluationEngine.evaluate(student, drive, role)).thenReturn(eval);

        RoleEligibilityEvaluationResponse res = service.evaluateAndPersistForRole(10L, 101L);

        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());
        verify(eligibilityResultRepository).save(existingResult);
        assertEquals(EligibilityDecision.ELIGIBLE, existingResult.getDecision());
    }
}

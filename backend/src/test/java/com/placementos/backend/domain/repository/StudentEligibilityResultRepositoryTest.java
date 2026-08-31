package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.StudentEligibilityResult;
import com.placementos.backend.domain.enums.DriveStatus;
import com.placementos.backend.domain.enums.EligibilityDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class StudentEligibilityResultRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlacementDriveRepository placementDriveRepository;

    @Autowired
    private PlacementRoleRepository placementRoleRepository;

    @Autowired
    private StudentEligibilityResultRepository eligibilityResultRepository;

    @Test
    void saveAndFind_persistsEligibilityResultWithJsonbCriteria() {
        // 1. Create Student
        Student student = new Student();
        student.setRegistrationNumber("21BCE2001");
        student.setName("Alice");
        student.setBranch("CSE");
        student.setBatch(2027);
        student.setCgpa(new BigDecimal("8.75"));
        student.setDegree("B.Tech");
        student.setSpecialization("AI");
        student.setStandingArrears(0);
        student.setGender("FEMALE");
        Student savedStudent = studentRepository.saveAndFlush(student);

        // 2. Create Placement Drive & Role
        PlacementDrive drive = new PlacementDrive();
        drive.setCompanyName("Acme Corp");
        drive.setTitle("Acme Campus Hiring 2027");
        drive.setSourceEmailId("msg-acme-repo-test-1");
        drive.setStatus(DriveStatus.OPEN);
        drive.setEligibilityCriteria(Map.of("minimumCgpa", 7.5));
        PlacementDrive savedDrive = placementDriveRepository.saveAndFlush(drive);

        PlacementRole role = new PlacementRole("Software Engineer", 1);
        role.setPlacementDrive(savedDrive);
        role.setEligibilityCriteria(Map.of("allowedBranches", List.of("CSE", "IT")));
        PlacementRole savedRole = placementRoleRepository.saveAndFlush(role);

        // 3. Save StudentEligibilityResult
        StudentEligibilityResult result = new StudentEligibilityResult();
        result.setStudent(savedStudent);
        result.setPlacementDrive(savedDrive);
        result.setPlacementRole(savedRole);
        result.setDecision(EligibilityDecision.ELIGIBLE);
        result.setEvaluatorVersion("eligibility-v1");
        result.setEvaluatedAt(Instant.now());
        result.setCriteriaResults(List.of(
                Map.of(
                        "criterion", "MINIMUM_CGPA",
                        "status", "PASS",
                        "scope", "DRIVE_COMMON",
                        "requiredValue", 7.5,
                        "actualValue", 8.75,
                        "reason", "Student CGPA 8.75 meets required minimum CGPA 7.5."
                ),
                Map.of(
                        "criterion", "ALLOWED_BRANCHES",
                        "status", "PASS",
                        "scope", "ROLE_SPECIFIC",
                        "requiredValue", List.of("CSE", "IT"),
                        "actualValue", "CSE",
                        "reason", "Student branch 'CSE' matches allowed branches: CSE, IT."
                )
        ));

        StudentEligibilityResult savedResult = eligibilityResultRepository.saveAndFlush(result);
        assertNotNull(savedResult.getId());

        // 4. Retrieve by student and role
        Optional<StudentEligibilityResult> foundOpt = eligibilityResultRepository
                .findByStudentIdAndPlacementRoleId(savedStudent.getId(), savedRole.getId());

        assertTrue(foundOpt.isPresent());
        StudentEligibilityResult found = foundOpt.get();
        assertEquals(EligibilityDecision.ELIGIBLE, found.getDecision());
        assertEquals("eligibility-v1", found.getEvaluatorVersion());
        assertEquals(2, found.getCriteriaResults().size());

        // 5. Retrieve by drive
        List<StudentEligibilityResult> driveResults = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(savedStudent.getId(), savedDrive.getId());
        assertEquals(1, driveResults.size());
    }
}

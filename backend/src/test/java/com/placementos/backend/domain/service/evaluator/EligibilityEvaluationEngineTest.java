package com.placementos.backend.domain.service.evaluator;

import com.placementos.backend.domain.dto.CriterionEvaluationResult;
import com.placementos.backend.domain.dto.RoleEligibilityEvaluationResponse;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.CriterionEvaluationStatus;
import com.placementos.backend.domain.enums.CriterionType;
import com.placementos.backend.domain.enums.EligibilityDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EligibilityEvaluationEngineTest {

    private EligibilityEvaluationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EligibilityEvaluationEngine();
    }

    // =========================================================================
    // CGPA Criterion Tests
    // =========================================================================

    @Test
    @DisplayName("CGPA: Minimum pass when student CGPA >= required")
    void cgpa_minimumPass() {
        Student student = createStudent("21BCE1001", "CSE", 2027, "8.50");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("minimumCgpa", 7.5));
        PlacementRole role = new PlacementRole("Software Engineer", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());
        assertEquals(1, res.getCriteriaResults().size());
        assertEquals(CriterionEvaluationStatus.PASS, res.getCriteriaResults().get(0).getStatus());
        assertTrue(res.getCriteriaResults().get(0).getReason().contains("meets required minimum"));
    }

    @Test
    @DisplayName("CGPA: Minimum fail when student CGPA < required")
    void cgpa_minimumFail() {
        Student student = createStudent("21BCE1002", "CSE", 2027, "7.20");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("minimumCgpa", 7.5));
        PlacementRole role = new PlacementRole("Software Engineer", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.NOT_ELIGIBLE, res.getDecision());
        assertEquals(1, res.getCriteriaResults().size());
        assertEquals(CriterionEvaluationStatus.FAIL, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("CGPA: Missing student CGPA results in UNKNOWN and REVIEW_REQUIRED")
    void cgpa_missingStudentCgpa_resultsInUnknown() {
        Student student = createStudent("21BCE1003", "CSE", 2027, null);
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("minimumCgpa", 7.0));
        PlacementRole role = new PlacementRole("Software Engineer", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.REVIEW_REQUIRED, res.getDecision());
        assertEquals(CriterionEvaluationStatus.UNKNOWN, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("CGPA: Maximum pass and fail")
    void cgpa_maximumCheck() {
        Student student = createStudent("21BCE1004", "CSE", 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("maximumCgpa", 8.5));
        PlacementRole role = new PlacementRole("Intern", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());

        drive.setEligibilityCriteria(Map.of("maximumCgpa", 7.5));
        RoleEligibilityEvaluationResponse resFail = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, resFail.getDecision());
    }

    // =========================================================================
    // Branch Criterion & Normalization Tests
    // =========================================================================

    @Test
    @DisplayName("Branch: Matching normalized branches pass")
    void branch_matchingNormalized() {
        Student student = createStudent("21BCE1005", "Computer Science and Engineering", 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("allowedBranches", List.of("CSE", "IT")));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.PASS, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Branch: Non-matching branch fails")
    void branch_nonMatchingFails() {
        Student student = createStudent("21BCE1006", "ECE", 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("allowedBranches", List.of("CSE", "IT")));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.NOT_ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.FAIL, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Branch: Missing student branch results in UNKNOWN and REVIEW_REQUIRED")
    void branch_missingBranch() {
        Student student = createStudent("21BCE1007", null, 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("allowedBranches", List.of("CSE")));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.REVIEW_REQUIRED, res.getDecision());
        assertEquals(CriterionEvaluationStatus.UNKNOWN, res.getCriteriaResults().get(0).getStatus());
    }

    // =========================================================================
    // Specialization Criterion Tests
    // =========================================================================

    @Test
    @DisplayName("Specialization: Exact and normalized matching pass")
    void specialization_matching() {
        Student student = createStudent("21BCE1008", "CSE", 2027, "8.00");
        student.setSpecialization("AI & ML");

        PlacementDrive drive = new PlacementDrive();
        PlacementRole role = new PlacementRole("AI Engineer", 1);
        role.setEligibilityCriteria(Map.of("allowedSpecializations", List.of("Artificial Intelligence")));

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.PASS, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Specialization: Mismatched specialization fails")
    void specialization_mismatch() {
        Student student = createStudent("21BCE1009", "CSE", 2027, "8.00");
        student.setSpecialization("Information Systems");

        PlacementDrive drive = new PlacementDrive();
        PlacementRole role = new PlacementRole("AI Engineer", 1);
        role.setEligibilityCriteria(Map.of("allowedSpecializations", List.of("Artificial Intelligence")));

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.NOT_ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.FAIL, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Specialization: Missing student specialization results in UNKNOWN")
    void specialization_missing() {
        Student student = createStudent("21BCE1010", "CSE", 2027, "8.00");
        student.setSpecialization(null);

        PlacementDrive drive = new PlacementDrive();
        PlacementRole role = new PlacementRole("AI Engineer", 1);
        role.setEligibilityCriteria(Map.of("allowedSpecializations", List.of("Artificial Intelligence")));

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.REVIEW_REQUIRED, res.getDecision());
        assertEquals(CriterionEvaluationStatus.UNKNOWN, res.getCriteriaResults().get(0).getStatus());
    }

    // =========================================================================
    // Standing Arrears Tests
    // =========================================================================

    @Test
    @DisplayName("Arrears: 0 standing arrears passes")
    void arrears_zeroPasses() {
        Student student = createStudent("21BCE1011", "CSE", 2027, "8.00");
        student.setStandingArrears(0);

        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("noStandingArrears", true));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.PASS, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Arrears: >0 standing arrears fails")
    void arrears_greaterThanZeroFails() {
        Student student = createStudent("21BCE1012", "CSE", 2027, "8.00");
        student.setStandingArrears(1);

        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("noStandingArrears", true));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.NOT_ELIGIBLE, res.getDecision());
        assertEquals(CriterionEvaluationStatus.FAIL, res.getCriteriaResults().get(0).getStatus());
    }

    @Test
    @DisplayName("Arrears: Null standing arrears is UNKNOWN -> REVIEW_REQUIRED")
    void arrears_nullIsUnknown() {
        Student student = createStudent("21BCE1013", "CSE", 2027, "8.00");
        student.setStandingArrears(null);

        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("noStandingArrears", true));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.REVIEW_REQUIRED, res.getDecision());
        assertEquals(CriterionEvaluationStatus.UNKNOWN, res.getCriteriaResults().get(0).getStatus());
    }

    // =========================================================================
    // Degree, Batch, and Gender Tests
    // =========================================================================

    @Test
    @DisplayName("Degree: B.Tech vs MCA normalization and matching")
    void degree_evaluation() {
        Student student = createStudent("21BCE1014", "CSE", 2027, "8.00");
        student.setDegree("BTech");

        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("allowedDegrees", List.of("B.Tech", "B.E.")));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());

        // Student with MCA
        student.setDegree("MCA");
        RoleEligibilityEvaluationResponse resMca = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, resMca.getDecision());
    }

    @Test
    @DisplayName("Batch: Graduation year matching and mismatch")
    void batch_evaluation() {
        Student student = createStudent("21BCE1015", "CSE", 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("allowedBatches", List.of("2027")));
        PlacementRole role = new PlacementRole("SWE", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());

        student.setBatch(2026);
        RoleEligibilityEvaluationResponse resFail = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, resFail.getDecision());
    }

    @Test
    @DisplayName("Gender: Gender restriction matching and mismatch")
    void gender_evaluation() {
        Student student = createStudent("21BCE1016", "CSE", 2027, "8.00");
        student.setGender("FEMALE");

        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("genderRestriction", "FEMALE"));
        PlacementRole role = new PlacementRole("Diversity Hiring", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.ELIGIBLE, res.getDecision());

        student.setGender("MALE");
        RoleEligibilityEvaluationResponse resFail = engine.evaluate(student, drive, role);
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, resFail.getDecision());
    }

    @Test
    @DisplayName("OtherConditions: Free text evaluates to UNSUPPORTED and REVIEW_REQUIRED")
    void otherConditions_unsupported() {
        Student student = createStudent("21BCE1017", "CSE", 2027, "8.00");
        PlacementDrive drive = new PlacementDrive();
        drive.setEligibilityCriteria(Map.of("otherConditions", List.of("Must possess a valid passport")));
        PlacementRole role = new PlacementRole("Consultant", 1);

        RoleEligibilityEvaluationResponse res = engine.evaluate(student, drive, role);

        assertEquals(EligibilityDecision.REVIEW_REQUIRED, res.getDecision());
        assertEquals(CriterionEvaluationStatus.UNSUPPORTED, res.getCriteriaResults().get(0).getStatus());
        assertTrue(res.getCriteriaResults().get(0).getReason().contains("requires manual review"));
    }

    // =========================================================================
    // Realistic Multi-Student, Multi-Role Fixtures (Task 25)
    // =========================================================================

    @Test
    @DisplayName("Realistic Fixtures: Example Technologies Drive with Software Engineer and Data Scientist roles")
    void realisticFixtures_task25() {
        // Placement Drive: Example Technologies
        PlacementDrive drive = new PlacementDrive();
        drive.setId(101L);
        drive.setCompanyName("Example Technologies");
        drive.setTitle("Example Technologies Campus Hiring 2027");
        drive.setEligibilityCriteria(Map.of(
                "minimumCgpa", 7.0,
                "allowedBranches", List.of("CSE", "IT"),
                "allowedBatches", List.of("2027"),
                "noStandingArrears", true
        ));

        // Role 1: Software Engineer (no extra criteria)
        PlacementRole sweRole = new PlacementRole("Software Engineer", 1);
        sweRole.setId(201L);
        sweRole.setPlacementDrive(drive);
        drive.addRole(sweRole);

        // Role 2: Data Scientist (extra: CGPA 7.5 + Specialization AI or Data Science)
        PlacementRole dsRole = new PlacementRole("Data Scientist", 2);
        dsRole.setId(202L);
        dsRole.setPlacementDrive(drive);
        dsRole.setEligibilityCriteria(Map.of(
                "minimumCgpa", 7.5,
                "allowedSpecializations", List.of("Artificial Intelligence", "Data Science")
        ));
        drive.addRole(dsRole);

        // Student A: CSE, 8.20 CGPA, 2027 batch, AI specialization, 0 standing arrears
        Student studentA = createStudent("21BCE0001", "CSE", 2027, "8.20");
        studentA.setSpecialization("Artificial Intelligence");
        studentA.setStandingArrears(0);

        RoleEligibilityEvaluationResponse aSwe = engine.evaluate(studentA, drive, sweRole);
        RoleEligibilityEvaluationResponse aDs = engine.evaluate(studentA, drive, dsRole);
        assertEquals(EligibilityDecision.ELIGIBLE, aSwe.getDecision(), "Student A should be ELIGIBLE for SWE");
        assertEquals(EligibilityDecision.ELIGIBLE, aDs.getDecision(), "Student A should be ELIGIBLE for Data Scientist");

        // Student B: IT, 7.20 CGPA, 2027 batch, Information Systems specialization, 0 standing arrears
        Student studentB = createStudent("21BIT0002", "IT", 2027, "7.20");
        studentB.setSpecialization("Information Systems");
        studentB.setStandingArrears(0);

        RoleEligibilityEvaluationResponse bSwe = engine.evaluate(studentB, drive, sweRole);
        RoleEligibilityEvaluationResponse bDs = engine.evaluate(studentB, drive, dsRole);
        assertEquals(EligibilityDecision.ELIGIBLE, bSwe.getDecision(), "Student B should be ELIGIBLE for SWE");
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, bDs.getDecision(), "Student B should be NOT_ELIGIBLE for Data Scientist (CGPA 7.2 < 7.5 and spec mismatch)");

        // Student C: ECE, 8.90 CGPA, 2027 batch, AI specialization, 0 standing arrears
        Student studentC = createStudent("21BEC0003", "ECE", 2027, "8.90");
        studentC.setSpecialization("Artificial Intelligence");
        studentC.setStandingArrears(0);

        RoleEligibilityEvaluationResponse cSwe = engine.evaluate(studentC, drive, sweRole);
        RoleEligibilityEvaluationResponse cDs = engine.evaluate(studentC, drive, dsRole);
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, cSwe.getDecision(), "Student C should be NOT_ELIGIBLE for SWE (Branch ECE not allowed)");
        assertEquals(EligibilityDecision.NOT_ELIGIBLE, cDs.getDecision(), "Student C should be NOT_ELIGIBLE for Data Scientist (Common branch failed)");

        // Student D: CSE, missing CGPA, 2027 batch, AI specialization, 0 standing arrears
        Student studentD = createStudent("21BCE0004", "CSE", 2027, null);
        studentD.setSpecialization("Artificial Intelligence");
        studentD.setStandingArrears(0);

        RoleEligibilityEvaluationResponse dSwe = engine.evaluate(studentD, drive, sweRole);
        RoleEligibilityEvaluationResponse dDs = engine.evaluate(studentD, drive, dsRole);
        assertEquals(EligibilityDecision.REVIEW_REQUIRED, dSwe.getDecision(), "Student D should be REVIEW_REQUIRED for SWE (Missing CGPA)");
        assertEquals(EligibilityDecision.REVIEW_REQUIRED, dDs.getDecision(), "Student D should be REVIEW_REQUIRED for Data Scientist (Missing CGPA)");
    }

    private Student createStudent(String regNo, String branch, Integer batch, String cgpaStr) {
        Student s = new Student();
        s.setId(1L);
        s.setRegistrationNumber(regNo);
        s.setName("Test Student " + regNo);
        s.setBranch(branch);
        s.setBatch(batch);
        s.setCgpa(cgpaStr != null ? new BigDecimal(cgpaStr) : null);
        return s;
    }
}

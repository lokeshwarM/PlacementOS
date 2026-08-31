package com.placementos.backend.domain.service.evaluator;

import com.placementos.backend.domain.dto.CriterionEvaluationResult;
import com.placementos.backend.domain.dto.RoleEligibilityEvaluationResponse;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.CriterionEvaluationStatus;
import com.placementos.backend.domain.enums.CriterionScope;
import com.placementos.backend.domain.enums.CriterionType;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.normalizer.BranchNormalizer;
import com.placementos.backend.domain.normalizer.DegreeNormalizer;
import com.placementos.backend.domain.normalizer.SpecializationNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Domain engine responsible for evaluating a student profile against common drive eligibility
 * and role-specific eligibility criteria, producing explainable criterion results and a final decision.
 *
 * Evaluator Version: eligibility-v1
 */
@Component
public class EligibilityEvaluationEngine {

    public static final String EVALUATOR_VERSION = "eligibility-v1";

    /**
     * Evaluates a student against a specific role within a placement drive.
     */
    public RoleEligibilityEvaluationResponse evaluate(Student student, PlacementDrive drive, PlacementRole role) {
        Objects.requireNonNull(student, "Student must not be null");
        Objects.requireNonNull(drive, "PlacementDrive must not be null");
        Objects.requireNonNull(role, "PlacementRole must not be null");

        List<CriterionEvaluationResult> criteriaResults = new ArrayList<>();

        // 1. Evaluate Drive-Level Common Criteria (DRIVE_COMMON)
        if (drive.getEligibilityCriteria() != null && !drive.getEligibilityCriteria().isEmpty()) {
            evaluateCriteriaMap(student, drive.getEligibilityCriteria(), CriterionScope.DRIVE_COMMON, criteriaResults);
        }

        // 2. Evaluate Role-Specific Criteria (ROLE_SPECIFIC)
        if (role.getEligibilityCriteria() != null && !role.getEligibilityCriteria().isEmpty()) {
            evaluateCriteriaMap(student, role.getEligibilityCriteria(), CriterionScope.ROLE_SPECIFIC, criteriaResults);
        }

        // 3. Aggregate Final Decision
        EligibilityDecision finalDecision = aggregateDecision(criteriaResults);

        RoleEligibilityEvaluationResponse response = new RoleEligibilityEvaluationResponse();
        response.setStudentId(student.getId());
        response.setPlacementDriveId(drive.getId());
        response.setPlacementRoleId(role.getId());
        response.setRoleTitle(role.getRoleTitle());
        response.setDecision(finalDecision);
        response.setCriteriaResults(criteriaResults);
        response.setEvaluatorVersion(EVALUATOR_VERSION);
        response.setEvaluatedAt(Instant.now());

        return response;
    }

    private void evaluateCriteriaMap(Student student,
                                    Map<String, Object> criteria,
                                    CriterionScope scope,
                                    List<CriterionEvaluationResult> results) {

        // 1. Minimum CGPA
        if (criteria.containsKey("minimumCgpa") && criteria.get("minimumCgpa") != null) {
            Object val = criteria.get("minimumCgpa");
            BigDecimal requiredMin = toBigDecimal(val);
            if (requiredMin != null) {
                if (student.getCgpa() == null) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MINIMUM_CGPA,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            requiredMin,
                            null,
                            "Student CGPA is not available in profile."
                    ));
                } else if (student.getCgpa().compareTo(requiredMin) >= 0) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MINIMUM_CGPA,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            requiredMin,
                            student.getCgpa(),
                            String.format("Student CGPA %s meets required minimum CGPA %s.", student.getCgpa(), requiredMin)
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MINIMUM_CGPA,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            requiredMin,
                            student.getCgpa(),
                            String.format("Student CGPA %s is below required minimum CGPA %s.", student.getCgpa(), requiredMin)
                    ));
                }
            }
        }

        // 2. Maximum CGPA
        if (criteria.containsKey("maximumCgpa") && criteria.get("maximumCgpa") != null) {
            Object val = criteria.get("maximumCgpa");
            BigDecimal requiredMax = toBigDecimal(val);
            if (requiredMax != null) {
                if (student.getCgpa() == null) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MAXIMUM_CGPA,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            requiredMax,
                            null,
                            "Student CGPA is not available in profile."
                    ));
                } else if (student.getCgpa().compareTo(requiredMax) <= 0) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MAXIMUM_CGPA,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            requiredMax,
                            student.getCgpa(),
                            String.format("Student CGPA %s is within maximum allowed CGPA %s.", student.getCgpa(), requiredMax)
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.MAXIMUM_CGPA,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            requiredMax,
                            student.getCgpa(),
                            String.format("Student CGPA %s exceeds maximum allowed CGPA %s.", student.getCgpa(), requiredMax)
                    ));
                }
            }
        }

        // 3. Allowed Branches
        if (criteria.containsKey("allowedBranches") && criteria.get("allowedBranches") != null) {
            List<String> allowedBranches = toStringList(criteria.get("allowedBranches"));
            if (allowedBranches != null && !allowedBranches.isEmpty()) {
                if (student.getBranch() == null || student.getBranch().isBlank()) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_BRANCHES,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            allowedBranches,
                            null,
                            "Student branch is not available in profile."
                    ));
                } else if (BranchNormalizer.matches(student.getBranch(), allowedBranches)) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_BRANCHES,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            allowedBranches,
                            student.getBranch(),
                            String.format("Student branch '%s' matches allowed branches: %s.", student.getBranch(), String.join(", ", allowedBranches))
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_BRANCHES,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            allowedBranches,
                            student.getBranch(),
                            String.format("Student branch '%s' is not among allowed branches: %s.", student.getBranch(), String.join(", ", allowedBranches))
                    ));
                }
            }
        }

        // 4. Allowed Specializations
        if (criteria.containsKey("allowedSpecializations") && criteria.get("allowedSpecializations") != null) {
            List<String> allowedSpecs = toStringList(criteria.get("allowedSpecializations"));
            if (allowedSpecs != null && !allowedSpecs.isEmpty()) {
                if (student.getSpecialization() == null || student.getSpecialization().isBlank()) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_SPECIALIZATIONS,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            allowedSpecs,
                            null,
                            "Student specialization is not available in profile."
                    ));
                } else if (SpecializationNormalizer.matches(student.getSpecialization(), allowedSpecs)) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_SPECIALIZATIONS,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            allowedSpecs,
                            student.getSpecialization(),
                            String.format("Student specialization '%s' matches allowed specializations: %s.", student.getSpecialization(), String.join(", ", allowedSpecs))
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_SPECIALIZATIONS,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            allowedSpecs,
                            student.getSpecialization(),
                            String.format("Student specialization '%s' is not among allowed specializations: %s.", student.getSpecialization(), String.join(", ", allowedSpecs))
                    ));
                }
            }
        }

        // 5. Allowed Batches
        if (criteria.containsKey("allowedBatches") && criteria.get("allowedBatches") != null) {
            List<String> allowedBatches = toStringList(criteria.get("allowedBatches"));
            if (allowedBatches != null && !allowedBatches.isEmpty()) {
                if (student.getBatch() == null) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_BATCHES,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            allowedBatches,
                            null,
                            "Student graduation batch is not available in profile."
                    ));
                } else {
                    String studentBatchStr = String.valueOf(student.getBatch());
                    boolean matches = allowedBatches.stream().anyMatch(b -> b.trim().equalsIgnoreCase(studentBatchStr));
                    if (matches) {
                        results.add(new CriterionEvaluationResult(
                                CriterionType.ALLOWED_BATCHES,
                                CriterionEvaluationStatus.PASS,
                                scope,
                                allowedBatches,
                                student.getBatch(),
                                String.format("Student batch %d matches allowed batches: %s.", student.getBatch(), String.join(", ", allowedBatches))
                        ));
                    } else {
                        results.add(new CriterionEvaluationResult(
                                CriterionType.ALLOWED_BATCHES,
                                CriterionEvaluationStatus.FAIL,
                                scope,
                                allowedBatches,
                                student.getBatch(),
                                String.format("Student batch %d is not among allowed batches: %s.", student.getBatch(), String.join(", ", allowedBatches))
                        ));
                    }
                }
            }
        }

        // 6. Allowed Degrees
        if (criteria.containsKey("allowedDegrees") && criteria.get("allowedDegrees") != null) {
            List<String> allowedDegrees = toStringList(criteria.get("allowedDegrees"));
            if (allowedDegrees != null && !allowedDegrees.isEmpty()) {
                if (student.getDegree() == null || student.getDegree().isBlank()) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_DEGREES,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            allowedDegrees,
                            null,
                            "Student degree is not available in profile."
                    ));
                } else if (DegreeNormalizer.matches(student.getDegree(), allowedDegrees)) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_DEGREES,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            allowedDegrees,
                            student.getDegree(),
                            String.format("Student degree '%s' matches allowed degrees: %s.", student.getDegree(), String.join(", ", allowedDegrees))
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.ALLOWED_DEGREES,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            allowedDegrees,
                            student.getDegree(),
                            String.format("Student degree '%s' is not among allowed degrees: %s.", student.getDegree(), String.join(", ", allowedDegrees))
                    ));
                }
            }
        }

        // 7. No Standing Arrears
        if (criteria.containsKey("noStandingArrears") && criteria.get("noStandingArrears") != null) {
            boolean requireNoArrears = Boolean.parseBoolean(String.valueOf(criteria.get("noStandingArrears")));
            if (requireNoArrears) {
                if (student.getStandingArrears() == null) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.NO_STANDING_ARREARS,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            true,
                            null,
                            "Student standing arrears information is not available."
                    ));
                } else if (student.getStandingArrears() == 0) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.NO_STANDING_ARREARS,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            true,
                            student.getStandingArrears(),
                            "Student has 0 standing arrears."
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.NO_STANDING_ARREARS,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            true,
                            student.getStandingArrears(),
                            String.format("Student has %d standing arrears, but 0 standing arrears are required.", student.getStandingArrears())
                    ));
                }
            }
        }

        // 8. Gender Restriction
        if (criteria.containsKey("genderRestriction") && criteria.get("genderRestriction") != null) {
            String restriction = String.valueOf(criteria.get("genderRestriction")).trim();
            if (!restriction.isBlank()) {
                if (student.getGender() == null || student.getGender().isBlank()) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.GENDER_RESTRICTION,
                            CriterionEvaluationStatus.UNKNOWN,
                            scope,
                            restriction,
                            null,
                            "Student gender is not available in profile."
                    ));
                } else if (student.getGender().trim().equalsIgnoreCase(restriction)) {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.GENDER_RESTRICTION,
                            CriterionEvaluationStatus.PASS,
                            scope,
                            restriction,
                            student.getGender(),
                            String.format("Student gender '%s' matches restriction: %s.", student.getGender(), restriction)
                    ));
                } else {
                    results.add(new CriterionEvaluationResult(
                            CriterionType.GENDER_RESTRICTION,
                            CriterionEvaluationStatus.FAIL,
                            scope,
                            restriction,
                            student.getGender(),
                            String.format("Student gender '%s' does not match requirement: %s.", student.getGender(), restriction)
                    ));
                }
            }
        }

        // 9. Other Conditions (Unsupported natural-language criteria)
        if (criteria.containsKey("otherConditions") && criteria.get("otherConditions") != null) {
            List<String> otherConditions = toStringList(criteria.get("otherConditions"));
            if (otherConditions != null) {
                for (String cond : otherConditions) {
                    if (cond != null && !cond.isBlank()) {
                        results.add(new CriterionEvaluationResult(
                                CriterionType.OTHER_CONDITIONS,
                                CriterionEvaluationStatus.UNSUPPORTED,
                                scope,
                                cond,
                                null,
                                String.format("Condition '%s' extracted but not automatically evaluable (requires manual review).", cond)
                        ));
                    }
                }
            }
        }
    }

    private EligibilityDecision aggregateDecision(List<CriterionEvaluationResult> criteriaResults) {
        if (criteriaResults == null || criteriaResults.isEmpty()) {
            return EligibilityDecision.ELIGIBLE;
        }

        // 1. Any FAIL -> NOT_ELIGIBLE
        boolean hasFail = criteriaResults.stream().anyMatch(r -> r.getStatus() == CriterionEvaluationStatus.FAIL);
        if (hasFail) {
            return EligibilityDecision.NOT_ELIGIBLE;
        }

        // 2. Any UNKNOWN or UNSUPPORTED -> REVIEW_REQUIRED
        boolean hasReviewRequired = criteriaResults.stream().anyMatch(r ->
                r.getStatus() == CriterionEvaluationStatus.UNKNOWN || r.getStatus() == CriterionEvaluationStatus.UNSUPPORTED);
        if (hasReviewRequired) {
            return EligibilityDecision.REVIEW_REQUIRED;
        }

        // 3. All PASS -> ELIGIBLE
        return EligibilityDecision.ELIGIBLE;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object val) {
        if (val == null) return null;
        if (val instanceof List) {
            List<?> rawList = (List<?>) val;
            List<String> res = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    res.add(String.valueOf(item).trim());
                }
            }
            return res;
        }
        return List.of(String.valueOf(val).trim());
    }
}

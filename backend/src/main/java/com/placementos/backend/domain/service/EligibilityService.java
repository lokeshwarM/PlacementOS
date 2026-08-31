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
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.PlacementRoleRepository;
import com.placementos.backend.domain.repository.StudentEligibilityResultRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import com.placementos.backend.domain.service.evaluator.EligibilityEvaluationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service orchestrating student eligibility evaluation and durable, idempotent persistence of results.
 */
@Service
@Transactional(readOnly = true)
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final StudentEligibilityResultRepository eligibilityResultRepository;
    private final EligibilityEvaluationEngine evaluationEngine;

    public EligibilityService(StudentRepository studentRepository,
                              PlacementDriveRepository placementDriveRepository,
                              PlacementRoleRepository placementRoleRepository,
                              StudentEligibilityResultRepository eligibilityResultRepository,
                              EligibilityEvaluationEngine evaluationEngine) {
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.eligibilityResultRepository = eligibilityResultRepository;
        this.evaluationEngine = evaluationEngine;
    }

    /**
     * Evaluates and idempotently persists eligibility for a student across all roles in a placement drive.
     */
    @Transactional
    public DriveEligibilityEvaluationResponse evaluateAndPersistForDrive(Long studentId, Long driveId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        List<PlacementRole> roles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(driveId);
        if (roles.isEmpty() && drive.getRoles() != null && !drive.getRoles().isEmpty()) {
            roles = drive.getRoles();
        }

        DriveEligibilityEvaluationResponse response = new DriveEligibilityEvaluationResponse(
                student.getId(),
                drive.getId(),
                drive.getCompanyName(),
                drive.getTitle()
        );

        if (roles.isEmpty()) {
            // Drive has no explicit roles; create a fallback role representation for evaluation
            PlacementRole fallbackRole = new PlacementRole(drive.getTitle() != null ? drive.getTitle() : "General Role", 1);
            fallbackRole.setId(-1L);
            fallbackRole.setPlacementDrive(drive);
            RoleEligibilityEvaluationResponse roleResp = evaluationEngine.evaluate(student, drive, fallbackRole);
            response.getRoleResults().add(roleResp);
            return response;
        }

        for (PlacementRole role : roles) {
            RoleEligibilityEvaluationResponse roleResult = evaluationEngine.evaluate(student, drive, role);
            persistEvaluationResult(student, drive, role, roleResult);
            response.getRoleResults().add(roleResult);
        }

        log.info("Evaluated student {} for drive {} ({} roles).", studentId, driveId, roles.size());
        return response;
    }

    /**
     * Evaluates and idempotently persists eligibility for a student on a specific placement role.
     */
    @Transactional
    public RoleEligibilityEvaluationResponse evaluateAndPersistForRole(Long studentId, Long roleId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementRole role = placementRoleRepository.findById(roleId)
                .orElseThrow(() -> ResourceNotFoundException.placementRole(roleId));

        PlacementDrive drive = role.getPlacementDrive();
        if (drive == null) {
            throw new ResourceNotFoundException("PlacementDrive for role not found with id: " + roleId);
        }

        RoleEligibilityEvaluationResponse roleResult = evaluationEngine.evaluate(student, drive, role);
        persistEvaluationResult(student, drive, role, roleResult);

        log.info("Evaluated student {} for role {} (decision: {}).", studentId, roleId, roleResult.getDecision());
        return roleResult;
    }

    /**
     * Retrieves persisted eligibility results for a student and drive.
     */
    public DriveEligibilityEvaluationResponse getPersistedResultsForDrive(Long studentId, Long driveId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.student(studentId));

        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        List<StudentEligibilityResult> persistedList = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(studentId, driveId);

        DriveEligibilityEvaluationResponse response = new DriveEligibilityEvaluationResponse(
                student.getId(),
                drive.getId(),
                drive.getCompanyName(),
                drive.getTitle()
        );

        for (StudentEligibilityResult entity : persistedList) {
            response.getRoleResults().add(mapToResponse(entity));
        }

        return response;
    }

    /**
     * Retrieves persisted eligibility result for a student and role.
     */
    public RoleEligibilityEvaluationResponse getPersistedResultForRole(Long studentId, Long roleId) {
        StudentEligibilityResult entity = eligibilityResultRepository
                .findByStudentIdAndPlacementRoleId(studentId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentEligibilityResult for student " + studentId + " and role " + roleId));

        return mapToResponse(entity);
    }

    // -------------------------------------------------------------------------
    // Persistence & Mapping Helpers
    // -------------------------------------------------------------------------

    private void persistEvaluationResult(Student student,
                                        PlacementDrive drive,
                                        PlacementRole role,
                                        RoleEligibilityEvaluationResponse eval) {
        if (role.getId() == null || role.getId() < 0) {
            return; // Fallback transient role
        }

        StudentEligibilityResult entity = eligibilityResultRepository
                .findByStudentIdAndPlacementRoleId(student.getId(), role.getId())
                .orElseGet(StudentEligibilityResult::new);

        entity.setStudent(student);
        entity.setPlacementDrive(drive);
        entity.setPlacementRole(role);
        entity.setDecision(eval.getDecision());
        entity.setEvaluatorVersion(eval.getEvaluatorVersion());
        entity.setEvaluatedAt(eval.getEvaluatedAt() != null ? eval.getEvaluatedAt() : Instant.now());

        // Map criteria results to List<Map<String, Object>> for JSONB storage
        List<Map<String, Object>> mappedCriteria = new ArrayList<>();
        for (CriterionEvaluationResult cr : eval.getCriteriaResults()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("criterion", cr.getCriterion() != null ? cr.getCriterion().name() : null);
            m.put("status", cr.getStatus() != null ? cr.getStatus().name() : null);
            m.put("scope", cr.getScope() != null ? cr.getScope().name() : null);
            m.put("requiredValue", cr.getRequiredValue());
            m.put("actualValue", cr.getActualValue());
            m.put("reason", cr.getReason());
            mappedCriteria.add(m);
        }
        entity.setCriteriaResults(mappedCriteria);

        eligibilityResultRepository.save(entity);
    }

    private RoleEligibilityEvaluationResponse mapToResponse(StudentEligibilityResult entity) {
        RoleEligibilityEvaluationResponse resp = new RoleEligibilityEvaluationResponse();
        resp.setStudentId(entity.getStudent().getId());
        resp.setPlacementDriveId(entity.getPlacementDrive().getId());
        resp.setPlacementRoleId(entity.getPlacementRole().getId());
        resp.setRoleTitle(entity.getPlacementRole().getRoleTitle());
        resp.setDecision(entity.getDecision());
        resp.setEvaluatorVersion(entity.getEvaluatorVersion());
        resp.setEvaluatedAt(entity.getEvaluatedAt());

        List<CriterionEvaluationResult> criteria = new ArrayList<>();
        if (entity.getCriteriaResults() != null) {
            for (Map<String, Object> m : entity.getCriteriaResults()) {
                CriterionEvaluationResult cr = new CriterionEvaluationResult();
                if (m.get("criterion") != null) {
                    cr.setCriterion(CriterionType.valueOf((String) m.get("criterion")));
                }
                if (m.get("status") != null) {
                    cr.setStatus(CriterionEvaluationStatus.valueOf((String) m.get("status")));
                }
                if (m.get("scope") != null) {
                    cr.setScope(CriterionScope.valueOf((String) m.get("scope")));
                }
                cr.setRequiredValue(m.get("requiredValue"));
                cr.setActualValue(m.get("actualValue"));
                cr.setReason((String) m.get("reason"));
                criteria.add(cr);
            }
        }
        resp.setCriteriaResults(criteria);
        return resp;
    }
}

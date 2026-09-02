package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.EligibilityDecision;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JPA entity for the {@code student_eligibility_results} table.
 * Stores the explainable evaluation decision for a student on a specific role within a placement drive.
 *
 * Unique constraint: (student_id, placement_role_id)
 */
@Entity
@Table(
    name = "student_eligibility_results",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_role_eligibility", columnNames = {"student_id", "placement_role_id"})
    },
    indexes = {
        @Index(name = "idx_student_eligibility_student_id", columnList = "student_id"),
        @Index(name = "idx_student_eligibility_drive_id", columnList = "placement_drive_id"),
        @Index(name = "idx_student_eligibility_role_id", columnList = "placement_role_id")
    }
)
public class StudentEligibilityResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_role_id", nullable = false)
    private PlacementRole placementRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 50)
    private EligibilityDecision decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_results", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> criteriaResults = new ArrayList<>();

    @Column(name = "evaluator_version", nullable = false, length = 50)
    private String evaluatorVersion;

    @Column(name = "evaluated_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant evaluatedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (evaluatedAt == null) evaluatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public StudentEligibilityResult() {}

    public StudentEligibilityResult(Student student, PlacementDrive placementDrive, PlacementRole placementRole,
                                  EligibilityDecision decision, List<Map<String, Object>> criteriaResults, String evaluatorVersion) {
        this.student = student;
        this.placementDrive = placementDrive;
        this.placementRole = placementRole;
        this.decision = decision;
        this.criteriaResults = criteriaResults != null ? criteriaResults : new ArrayList<>();
        this.evaluatorVersion = evaluatorVersion;
        this.evaluatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public PlacementRole getPlacementRole() { return placementRole; }
    public void setPlacementRole(PlacementRole placementRole) { this.placementRole = placementRole; }

    public EligibilityDecision getDecision() { return decision; }
    public void setDecision(EligibilityDecision decision) { this.decision = decision; }

    public List<Map<String, Object>> getCriteriaResults() { return criteriaResults; }
    public void setCriteriaResults(List<Map<String, Object>> criteriaResults) { this.criteriaResults = criteriaResults; }

    public String getEvaluatorVersion() { return evaluatorVersion; }
    public void setEvaluatorVersion(String evaluatorVersion) { this.evaluatorVersion = evaluatorVersion; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

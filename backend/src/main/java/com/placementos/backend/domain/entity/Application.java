package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code applications} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * Composite UNIQUE constraint: (student_id, placement_drive_id)
 * This is the authoritative constraint; the database enforces it.
 *
 * FK: student_id       → students(id)          (required, no cascade)
 * FK: placement_drive_id → placement_drives(id) (required, no cascade)
 *
 * Audit/history note: no cascade delete — application records must not
 * disappear if a drive or student is deleted without explicit intent.
 */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_applications_student_drive",
                          columnNames = {"student_id", "placement_drive_id"})
    }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning student. LAZY to avoid loading the full student graph.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Associated placement drive. LAZY fetch.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    /**
     * Associated placement role (optional). Nullable for drive-level applications.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_role_id")
    private PlacementRole placementRole;

    /**
     * Application lifecycle status. Stored as VARCHAR(50) with CHECK constraint.
     * @see ApplicationStatus for valid values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status = ApplicationStatus.NOT_STARTED;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ApplicationStatus.NOT_STARTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public Application() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public PlacementRole getPlacementRole() { return placementRole; }
    public void setPlacementRole(PlacementRole placementRole) { this.placementRole = placementRole; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Application)) return false;
        Application other = (Application) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Application{id=" + id + ", status=" + status + "}";
    }
}

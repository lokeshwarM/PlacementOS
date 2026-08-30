package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.ReminderStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code reminder_tasks} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * Composite UNIQUE constraint: (student_id, placement_drive_id)
 * Prevents the same reminder being created twice for the same student+drive.
 *
 * FK: student_id         → students(id)          (required, no cascade)
 * FK: placement_drive_id → placement_drives(id)  (required, no cascade)
 *
 * No scheduler logic is implemented here.
 */
@Entity
@Table(
    name = "reminder_tasks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reminder_tasks_student_drive",
                          columnNames = {"student_id", "placement_drive_id"})
    }
)
public class ReminderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    /**
     * Reminder execution status. Stored as VARCHAR(50) with CHECK constraint.
     * @see ReminderStatus for valid values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

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
        if (status == null) status = ReminderStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public ReminderTask() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public Instant getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }

    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus status) { this.status = status; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReminderTask)) return false;
        ReminderTask other = (ReminderTask) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ReminderTask{id=" + id + ", scheduledFor=" + scheduledFor + ", status=" + status + "}";
    }
}

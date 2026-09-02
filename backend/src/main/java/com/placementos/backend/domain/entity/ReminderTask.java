package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.ReminderStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for the {@code reminder_tasks} table.
 * Represents a scheduled deadline reminder for a student and drive/role.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_role_id")
    private PlacementRole placementRole;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes = 60;

    @Column(name = "max_reminders", nullable = false)
    private Integer maxReminders = 5;

    @Column(name = "reminders_sent", nullable = false)
    private Integer remindersSent = 0;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

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

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ReminderStatus.PENDING;
        if (intervalMinutes == null) intervalMinutes = 60;
        if (maxReminders == null) maxReminders = 5;
        if (remindersSent == null) remindersSent = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public ReminderTask() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public PlacementRole getPlacementRole() { return placementRole; }
    public void setPlacementRole(PlacementRole placementRole) { this.placementRole = placementRole; }

    public Instant getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }

    public Integer getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(Integer intervalMinutes) { this.intervalMinutes = intervalMinutes; }

    public Integer getMaxReminders() { return maxReminders; }
    public void setMaxReminders(Integer maxReminders) { this.maxReminders = maxReminders; }

    public Integer getRemindersSent() { return remindersSent; }
    public void setRemindersSent(Integer remindersSent) { this.remindersSent = remindersSent; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Instant getLastReminderAt() { return lastReminderAt; }
    public void setLastReminderAt(Instant lastReminderAt) { this.lastReminderAt = lastReminderAt; }

    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus status) { this.status = status; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReminderTask other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ReminderTask{id=" + id + ", scheduledFor=" + scheduledFor + ", status=" + status
                + ", sent=" + remindersSent + "/" + maxReminders + "}";
    }
}

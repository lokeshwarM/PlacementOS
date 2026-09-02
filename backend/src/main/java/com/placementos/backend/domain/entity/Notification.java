package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.NotificationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for the {@code notifications} table.
 * Tracks personalised notifications dispatched to students.
 * Idempotency at the database level is handled by the unique {@code idempotency_key}.
 */
@Entity
@Table(
    name = "notifications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_notifications_idempotency_key", columnNames = {"idempotency_key"})
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_role_id")
    private PlacementRole placementRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "message_payload", columnDefinition = "TEXT")
    private String messagePayload;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = NotificationStatus.PENDING;
    }

    public Notification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public PlacementRole getPlacementRole() { return placementRole; }
    public void setPlacementRole(PlacementRole placementRole) { this.placementRole = placementRole; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public String getMessagePayload() { return messagePayload; }
    public void setMessagePayload(String messagePayload) { this.messagePayload = messagePayload; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", idempotencyKey='" + idempotencyKey + '\''
                + ", notificationType=" + notificationType + ", channel=" + channel + ", status=" + status + "}";
    }
}

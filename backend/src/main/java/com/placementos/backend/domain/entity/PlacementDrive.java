package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.DriveStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;

/**
 * JPA entity for the {@code placement_drives} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * eligibility_criteria is JSONB in PostgreSQL.
 * It is mapped as a Map<String, Object> for flexible read access.
 * No eligibility business logic is implemented here.
 *
 * Timestamps are TIMESTAMPTZ → Instant (UTC).
 */
@Entity
@Table(name = "placement_drives")
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "source_email_id", length = 255)
    private String sourceEmailId;

    /**
     * JSONB column for flexible eligibility criteria storage.
     * Mapped to Map<String, Object> to avoid requiring a rigid schema at this stage.
     * No eligibility evaluation logic is present here.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_criteria", columnDefinition = "JSONB")
    private Map<String, Object> eligibilityCriteria;

    /**
     * Drive lifecycle status. Stored as VARCHAR(50) with CHECK constraint.
     * @see DriveStatus for valid values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DriveStatus status = DriveStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    @OneToMany(mappedBy = "placementDrive", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roleOrder ASC")
    private java.util.List<PlacementRole> roles = new java.util.ArrayList<>();

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = DriveStatus.OPEN;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public PlacementDrive() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(Instant applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public String getSourceEmailId() { return sourceEmailId; }
    public void setSourceEmailId(String sourceEmailId) { this.sourceEmailId = sourceEmailId; }

    public Map<String, Object> getEligibilityCriteria() { return eligibilityCriteria; }
    public void setEligibilityCriteria(Map<String, Object> eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }

    public DriveStatus getStatus() { return status; }
    public void setStatus(DriveStatus status) { this.status = status; }

    public java.util.List<PlacementRole> getRoles() { return roles; }
    public void setRoles(java.util.List<PlacementRole> roles) { this.roles = roles; }

    public void addRole(PlacementRole role) {
        if (role != null) {
            roles.add(role);
            role.setPlacementDrive(this);
        }
    }

    public void removeRole(PlacementRole role) {
        if (role != null) {
            roles.remove(role);
            role.setPlacementDrive(null);
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlacementDrive)) return false;
        PlacementDrive other = (PlacementDrive) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PlacementDrive{id=" + id + ", companyName='" + companyName + "', status=" + status + "}";
    }
}

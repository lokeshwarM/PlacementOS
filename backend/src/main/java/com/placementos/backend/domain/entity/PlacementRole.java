package com.placementos.backend.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * JPA entity mapping the {@code placement_roles} table.
 * Represents an individual role/position in a placement drive.
 *
 * Role-specific eligibility is stored in {@code eligibility_criteria} (JSONB).
 * Common eligibility across all roles remains on the parent {@link PlacementDrive}.
 */
@Entity
@Table(name = "placement_roles")
public class PlacementRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @Column(name = "role_title", nullable = false, length = 255)
    private String roleTitle;

    @Column(name = "role_description", columnDefinition = "TEXT")
    private String roleDescription;

    @Column(name = "role_order", nullable = false)
    private Integer roleOrder = 1;

    /**
     * Role-specific eligibility constraints (JSONB).
     * If the role has no additional constraints beyond common drive eligibility,
     * this field may be null.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_criteria", columnDefinition = "JSONB")
    private Map<String, Object> eligibilityCriteria;

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
        if (roleOrder == null) roleOrder = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public PlacementRole() {}

    public PlacementRole(String roleTitle, Integer roleOrder) {
        this.roleTitle = roleTitle;
        this.roleOrder = roleOrder != null ? roleOrder : 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlacementDrive getPlacementDrive() {
        return placementDrive;
    }

    public void setPlacementDrive(PlacementDrive placementDrive) {
        this.placementDrive = placementDrive;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public Integer getRoleOrder() {
        return roleOrder;
    }

    public void setRoleOrder(Integer roleOrder) {
        this.roleOrder = roleOrder;
    }

    public Map<String, Object> getEligibilityCriteria() {
        return eligibilityCriteria;
    }

    public void setEligibilityCriteria(Map<String, Object> eligibilityCriteria) {
        this.eligibilityCriteria = eligibilityCriteria;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlacementRole that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PlacementRole{id=" + id + ", roleTitle='" + roleTitle + "', roleOrder=" + roleOrder + "}";
    }
}

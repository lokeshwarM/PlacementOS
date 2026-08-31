package com.placementos.backend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code students} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * Unique constraints:
 *  - registration_number (NOT NULL, UNIQUE)
 *  - neopat_id           (nullable, UNIQUE)
 *
 * CGPA is stored as NUMERIC(4,2) → BigDecimal.
 * Timestamps are TIMESTAMPTZ → Instant (UTC).
 */
@Entity
@Table(
    name = "students",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_students_registration_number", columnNames = "registration_number"),
        @UniqueConstraint(name = "uq_students_neopat_id",           columnNames = "neopat_id")
    }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "neopat_id", length = 20)
    private String neopatId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "branch", nullable = false, length = 100)
    private String branch;

    @Column(name = "batch", nullable = false)
    private Integer batch;

    /**
     * NUMERIC(4,2) in PostgreSQL. Stored as exact decimal; must not use float/double.
     * CHECK constraint: 0.00 ≤ cgpa ≤ 10.00 is enforced by the database.
     */
    @Column(name = "cgpa", nullable = false, precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "degree", length = 50)
    private String degree;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "standing_arrears")
    private Integer standingArrears;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Lifecycle hooks — keep timestamps in sync without requiring callers to set them.
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public Student() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Integer getBatch() { return batch; }
    public void setBatch(Integer batch) { this.batch = batch; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Integer getStandingArrears() { return standingArrears; }
    public void setStandingArrears(Integer standingArrears) { this.standingArrears = standingArrears; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode — based only on the database-assigned id.
    // Safe for use inside Hibernate-managed collections.
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student other = (Student) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Student{id=" + id + ", registrationNumber='" + registrationNumber + "', name='" + name + "'}";
    }
}

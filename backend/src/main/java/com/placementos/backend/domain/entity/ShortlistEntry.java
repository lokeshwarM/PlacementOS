package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code shortlist_entries} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * A shortlist entry may identify a candidate via any combination of:
 *   - registration_number
 *   - neopat_id
 *   - candidate_name
 * At least one must be present (enforced at the application layer).
 *
 * FK: placement_drive_id → placement_drives(id)   (required, no cascade)
 * FK: source_attachment_id → attachments(id)       (nullable, no cascade)
 * FK: student_id → students(id)                   (nullable, matched student)
 * FK: placement_role_id → placement_roles(id)     (nullable, role association)
 */
@Entity
@Table(name = "shortlist_entries")
public class ShortlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Required FK to the drive this shortlist belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    /**
     * Optional FK to the matched student.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Optional FK to the specific placement role if identified in shortlist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_role_id")
    private PlacementRole placementRole;

    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(name = "neopat_id", length = 20)
    private String neopatId;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    /**
     * Optional FK to the attachment this entry was extracted from.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_attachment_id")
    private Attachment sourceAttachment;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    private ShortlistMatchStatus matchStatus = ShortlistMatchStatus.UNMATCHED;

    @Column(name = "match_method", length = 50)
    private String matchMethod;

    @Column(name = "match_reason", columnDefinition = "TEXT")
    private String matchReason;

    @Column(name = "raw_evidence", columnDefinition = "TEXT")
    private String rawEvidence;

    /**
     * Confidence score for the match. NUMERIC(5,4) in PostgreSQL → BigDecimal.
     * Range: 0.0000–1.0000.
     */
    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

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
        if (matchStatus == null) matchStatus = ShortlistMatchStatus.UNMATCHED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public ShortlistEntry() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public PlacementRole getPlacementRole() { return placementRole; }
    public void setPlacementRole(PlacementRole placementRole) { this.placementRole = placementRole; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public Attachment getSourceAttachment() { return sourceAttachment; }
    public void setSourceAttachment(Attachment sourceAttachment) { this.sourceAttachment = sourceAttachment; }

    public ShortlistMatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(ShortlistMatchStatus matchStatus) { this.matchStatus = matchStatus; }

    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public String getRawEvidence() { return rawEvidence; }
    public void setRawEvidence(String rawEvidence) { this.rawEvidence = rawEvidence; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortlistEntry)) return false;
        ShortlistEntry other = (ShortlistEntry) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ShortlistEntry{id=" + id + ", registrationNumber='" + registrationNumber
                + "', neopatId='" + neopatId + "', candidateName='" + candidateName
                + "', matchStatus=" + matchStatus + "}";
    }
}

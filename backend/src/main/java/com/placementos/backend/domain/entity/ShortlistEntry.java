package com.placementos.backend.domain.entity;

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
 *
 * confidence is NUMERIC(5,4) → BigDecimal.
 */
@Entity
@Table(name = "shortlist_entries")
public class ShortlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Required FK to the drive this shortlist belongs to.
     * LAZY: avoid loading the entire drive on every shortlist query.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(name = "neopat_id", length = 20)
    private String neopatId;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    /**
     * Optional FK to the attachment this entry was extracted from.
     * LAZY: avoid loading the file record on every shortlist query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_attachment_id")
    private Attachment sourceAttachment;

    @Column(name = "match_method", length = 50)
    private String matchMethod;

    /**
     * Confidence score for the match. NUMERIC(5,4) in PostgreSQL → BigDecimal.
     * Range: 0.0000–1.0000.
     */
    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
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

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public Attachment getSourceAttachment() { return sourceAttachment; }
    public void setSourceAttachment(Attachment sourceAttachment) { this.sourceAttachment = sourceAttachment; }

    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public Instant getCreatedAt() { return createdAt; }

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
                + "', neopatId='" + neopatId + "', candidateName='" + candidateName + "'}";
    }
}

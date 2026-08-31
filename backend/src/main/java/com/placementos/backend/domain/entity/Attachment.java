package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code attachments} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * Files are NOT stored in the database.
 * storage_reference points to an external file path or object-store key.
 *
 * FK: placement_drive_id → placement_drives(id)  (no cascade delete: audit safety)
 */
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optional FK to placement_drives.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_drive_id")
    private PlacementDrive placementDrive;

    /**
     * Optional FK to gmail_messages.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gmail_message_record_id")
    private GmailMessage gmailMessage;

    @Column(name = "attachment_id", length = 255)
    private String attachmentId;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "storage_reference", columnDefinition = "TEXT")
    private String storageReference;

    /**
     * Parse status. Stored as VARCHAR(50) with CHECK constraint.
     * @see AttachmentParsedStatus for valid values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parsed_status", nullable = false, length = 50)
    private AttachmentParsedStatus parsedStatus = AttachmentParsedStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (parsedStatus == null) parsedStatus = AttachmentParsedStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public Attachment() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public GmailMessage getGmailMessage() { return gmailMessage; }
    public void setGmailMessage(GmailMessage gmailMessage) { this.gmailMessage = gmailMessage; }

    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }

    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long byteSize) { this.byteSize = byteSize; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getStorageReference() { return storageReference; }
    public void setStorageReference(String storageReference) { this.storageReference = storageReference; }

    public AttachmentParsedStatus getParsedStatus() { return parsedStatus; }
    public void setParsedStatus(AttachmentParsedStatus parsedStatus) { this.parsedStatus = parsedStatus; }

    public Instant getCreatedAt() { return createdAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment)) return false;
        Attachment other = (Attachment) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Attachment{id=" + id + ", filename='" + filename + "', parsedStatus=" + parsedStatus + "}";
    }
}

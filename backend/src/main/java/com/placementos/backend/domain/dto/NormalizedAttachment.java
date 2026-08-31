package com.placementos.backend.domain.dto;

/**
 * Normalized attachment metadata extracted from a Gmail MIME part.
 * Binary payload is intentionally NOT loaded or held here.
 */
public class NormalizedAttachment {

    private String filename;
    private String contentType;
    private String attachmentId;
    private Long byteSize;

    public NormalizedAttachment() {}

    public NormalizedAttachment(String filename, String contentType, String attachmentId, Long byteSize) {
        this.filename = filename;
        this.contentType = contentType;
        this.attachmentId = attachmentId;
        this.byteSize = byteSize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public Long getByteSize() {
        return byteSize;
    }

    public void setByteSize(Long byteSize) {
        this.byteSize = byteSize;
    }
}

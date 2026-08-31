package com.placementos.backend.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DocumentProcessingResultDto {
    private String documentId;
    private Long attachmentId;
    private String filename;
    private String documentType;
    private String classification;
    private Double confidence;
    private Boolean ocrRequired = false;
    private List<ShortlistCandidateDto> candidates = new ArrayList<>();
    private String errorMessage;

    public DocumentProcessingResultDto() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Boolean getOcrRequired() { return ocrRequired; }
    public void setOcrRequired(Boolean ocrRequired) { this.ocrRequired = ocrRequired; }

    public List<ShortlistCandidateDto> getCandidates() { return candidates; }
    public void setCandidates(List<ShortlistCandidateDto> candidates) { this.candidates = candidates; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

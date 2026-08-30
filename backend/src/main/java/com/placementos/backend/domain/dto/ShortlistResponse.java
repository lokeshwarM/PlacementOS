package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.ShortlistEntry;

import java.math.BigDecimal;
import java.time.Instant;

public class ShortlistResponse {

    private Long id;
    private Long placementDriveId;
    private Long sourceAttachmentId;
    private String registrationNumber;
    private String neopatId;
    private String candidateName;
    private String matchMethod;
    private BigDecimal confidence;
    private Instant createdAt;

    public static ShortlistResponse from(ShortlistEntry entry) {
        ShortlistResponse response = new ShortlistResponse();
        response.id = entry.getId();
        response.placementDriveId = entry.getPlacementDrive().getId();
        response.sourceAttachmentId = entry.getSourceAttachment() != null ? entry.getSourceAttachment().getId() : null;
        response.registrationNumber = entry.getRegistrationNumber();
        response.neopatId = entry.getNeopatId();
        response.candidateName = entry.getCandidateName();
        response.matchMethod = entry.getMatchMethod();
        response.confidence = entry.getConfidence();
        response.createdAt = entry.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getPlacementDriveId() { return placementDriveId; }
    public Long getSourceAttachmentId() { return sourceAttachmentId; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getNeopatId() { return neopatId; }
    public String getCandidateName() { return candidateName; }
    public String getMatchMethod() { return matchMethod; }
    public BigDecimal getConfidence() { return confidence; }
    public Instant getCreatedAt() { return createdAt; }
}

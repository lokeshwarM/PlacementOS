package com.placementos.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassificationResultDto {

    private Boolean isPlacement;
    private Double confidence;
    private List<String> evidence;

    public ClassificationResultDto() {}

    public ClassificationResultDto(Boolean isPlacement, Double confidence, List<String> evidence) {
        this.isPlacement = isPlacement;
        this.confidence = confidence;
        this.evidence = evidence;
    }

    public Boolean getIsPlacement() { return isPlacement; }
    public void setIsPlacement(Boolean isPlacement) { this.isPlacement = isPlacement; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}

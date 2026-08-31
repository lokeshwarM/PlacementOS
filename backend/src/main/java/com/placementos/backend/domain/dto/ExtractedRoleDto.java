package com.placementos.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtractedRoleDto {

    private String title;
    private String description;
    private Integer roleOrder;
    private StructuredEligibilityDto eligibility;
    private Map<String, String> evidence;

    public ExtractedRoleDto() {}

    public ExtractedRoleDto(String title, Integer roleOrder) {
        this.title = title;
        this.roleOrder = roleOrder;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRoleOrder() { return roleOrder; }
    public void setRoleOrder(Integer roleOrder) { this.roleOrder = roleOrder; }

    public StructuredEligibilityDto getEligibility() { return eligibility; }
    public void setEligibility(StructuredEligibilityDto eligibility) { this.eligibility = eligibility; }

    public Map<String, String> getEvidence() { return evidence; }
    public void setEvidence(Map<String, String> evidence) { this.evidence = evidence; }
}

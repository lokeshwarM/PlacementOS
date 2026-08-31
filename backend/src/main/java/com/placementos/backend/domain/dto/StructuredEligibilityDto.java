package com.placementos.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured representation of eligibility criteria.
 * Unspecified fields remain null. No assumptions/restrictions are inferred.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredEligibilityDto {

    private Double minimumCgpa;
    private Double maximumCgpa;
    private List<String> allowedBranches;
    private List<String> allowedSpecializations;
    private List<String> allowedBatches;
    private List<String> allowedDegrees;
    private String genderRestriction;
    private Boolean noStandingArrears;
    private List<String> otherConditions;

    public StructuredEligibilityDto() {}

    public Double getMinimumCgpa() { return minimumCgpa; }
    public void setMinimumCgpa(Double minimumCgpa) { this.minimumCgpa = minimumCgpa; }

    public Double getMaximumCgpa() { return maximumCgpa; }
    public void setMaximumCgpa(Double maximumCgpa) { this.maximumCgpa = maximumCgpa; }

    public List<String> getAllowedBranches() { return allowedBranches; }
    public void setAllowedBranches(List<String> allowedBranches) { this.allowedBranches = allowedBranches; }

    public List<String> getAllowedSpecializations() { return allowedSpecializations; }
    public void setAllowedSpecializations(List<String> allowedSpecializations) { this.allowedSpecializations = allowedSpecializations; }

    public List<String> getAllowedBatches() { return allowedBatches; }
    public void setAllowedBatches(List<String> allowedBatches) { this.allowedBatches = allowedBatches; }

    public List<String> getAllowedDegrees() { return allowedDegrees; }
    public void setAllowedDegrees(List<String> allowedDegrees) { this.allowedDegrees = allowedDegrees; }

    public String getGenderRestriction() { return genderRestriction; }
    public void setGenderRestriction(String genderRestriction) { this.genderRestriction = genderRestriction; }

    public Boolean getNoStandingArrears() { return noStandingArrears; }
    public void setNoStandingArrears(Boolean noStandingArrears) { this.noStandingArrears = noStandingArrears; }

    public List<String> getOtherConditions() { return otherConditions; }
    public void setOtherConditions(List<String> otherConditions) { this.otherConditions = otherConditions; }

    /**
     * Converts non-null fields to a Map suitable for JSONB storage in PostgreSQL.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (minimumCgpa != null) map.put("minimumCgpa", minimumCgpa);
        if (maximumCgpa != null) map.put("maximumCgpa", maximumCgpa);
        if (allowedBranches != null && !allowedBranches.isEmpty()) map.put("allowedBranches", allowedBranches);
        if (allowedSpecializations != null && !allowedSpecializations.isEmpty()) map.put("allowedSpecializations", allowedSpecializations);
        if (allowedBatches != null && !allowedBatches.isEmpty()) map.put("allowedBatches", allowedBatches);
        if (allowedDegrees != null && !allowedDegrees.isEmpty()) map.put("allowedDegrees", allowedDegrees);
        if (genderRestriction != null) map.put("genderRestriction", genderRestriction);
        if (noStandingArrears != null) map.put("noStandingArrears", noStandingArrears);
        if (otherConditions != null && !otherConditions.isEmpty()) map.put("otherConditions", otherConditions);
        return map.isEmpty() ? null : map;
    }
}

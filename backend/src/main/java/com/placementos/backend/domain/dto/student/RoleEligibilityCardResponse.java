package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.enums.EligibilityDecision;

import java.math.BigDecimal;
import java.util.List;

public record RoleEligibilityCardResponse(
        Long roleId,
        String roleTitle,
        EligibilityDecision decision,
        List<String> criteriaExplanations,
        BigDecimal minCgpa,
        List<String> eligibleBranches,
        Integer maxStandingArrearsAllowed,
        String genderAllowed
) {}

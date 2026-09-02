package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;

import java.time.Instant;
import java.util.List;

public record StudentPlacementDriveCardResponse(
        Long driveId,
        String companyName,
        String title,
        String description,
        Instant applicationDeadline,
        Instant driveDate,
        EligibilityDecision overallEligibility,
        ApplicationStatus applicationStatus,
        Long applicationId,
        ShortlistMatchStatus shortlistStatus,
        List<RoleEligibilityCardResponse> roles,
        String actionRequired,
        boolean isActionable,
        boolean deadlinePassed
) {}

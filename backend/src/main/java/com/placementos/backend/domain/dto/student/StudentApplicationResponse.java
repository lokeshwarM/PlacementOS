package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;

import java.time.Instant;

public record StudentApplicationResponse(
        Long applicationId,
        Long driveId,
        String companyName,
        String driveTitle,
        Long roleId,
        String roleTitle,
        ApplicationStatus status,
        Instant appliedAt,
        ShortlistMatchStatus shortlistStatus,
        Instant deadline,
        Instant createdAt
) {}

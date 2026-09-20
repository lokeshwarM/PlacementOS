package com.placementos.backend.domain.dto.student;

import java.time.Instant;

public record TelegramLinkResponse(
        String token,
        String deepLink,
        Instant expiresAt
) {}

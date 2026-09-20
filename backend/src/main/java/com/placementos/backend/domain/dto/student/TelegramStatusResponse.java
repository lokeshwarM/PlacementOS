package com.placementos.backend.domain.dto.student;

import java.time.Instant;

public record TelegramStatusResponse(
        boolean linked,
        String telegramUsername,
        Instant linkedAt
) {}

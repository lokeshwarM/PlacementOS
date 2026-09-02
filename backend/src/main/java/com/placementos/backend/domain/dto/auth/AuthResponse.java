package com.placementos.backend.domain.dto.auth;

import com.placementos.backend.domain.enums.ProfileStatus;
import com.placementos.backend.domain.enums.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        UserRole role,
        ProfileStatus profileStatus,
        Long studentId,
        String studentName
) {
    public static AuthResponse of(String token, Long userId, String email, UserRole role,
                                 ProfileStatus profileStatus, Long studentId, String studentName) {
        return new AuthResponse(token, "Bearer", userId, email, role, profileStatus, studentId, studentName);
    }
}

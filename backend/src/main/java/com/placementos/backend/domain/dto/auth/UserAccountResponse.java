package com.placementos.backend.domain.dto.auth;

import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.ProfileStatus;
import com.placementos.backend.domain.enums.UserRole;

public record UserAccountResponse(
        Long id,
        String email,
        UserRole role,
        ProfileStatus profileStatus,
        Long studentId,
        String studentName,
        String registrationNumber,
        String branch,
        Integer batch
) {
    public static UserAccountResponse from(UserAccount user) {
        Student student = user.getStudent();
        return new UserAccountResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getProfileStatus(),
                student != null ? student.getId() : null,
                student != null ? student.getName() : null,
                student != null ? student.getRegistrationNumber() : null,
                student != null ? student.getBranch() : null,
                student != null ? student.getBatch() : null
        );
    }
}

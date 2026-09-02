package com.placementos.backend.domain.dto.student;

import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.ProfileStatus;

import java.math.BigDecimal;

public record StudentProfileResponse(
        Long studentId,
        Long userId,
        String email,
        String name,
        String registrationNumber,
        String neopatId,
        String branch,
        Integer batch,
        BigDecimal cgpa,
        String phoneNumber,
        String degree,
        String specialization,
        Integer standingArrears,
        String gender,
        ProfileStatus profileStatus,
        boolean isProfileComplete
) {
    public static StudentProfileResponse from(UserAccount user) {
        Student student = user.getStudent();
        boolean isComplete = user.getProfileStatus() == ProfileStatus.COMPLETE || user.getProfileStatus() == ProfileStatus.VERIFIED;
        if (student == null) {
            return new StudentProfileResponse(
                    null, user.getId(), user.getEmail(), null, null, null,
                    null, null, null, null, null, null, null, null,
                    user.getProfileStatus(), false
            );
        }

        return new StudentProfileResponse(
                student.getId(),
                user.getId(),
                user.getEmail(),
                student.getName(),
                student.getRegistrationNumber(),
                student.getNeopatId(),
                student.getBranch(),
                student.getBatch(),
                student.getCgpa(),
                student.getPhoneNumber(),
                student.getDegree(),
                student.getSpecialization(),
                student.getStandingArrears(),
                student.getGender(),
                user.getProfileStatus(),
                isComplete
        );
    }
}

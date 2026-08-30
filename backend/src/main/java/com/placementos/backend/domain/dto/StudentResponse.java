package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.entity.Student;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound DTO for returning Student data to API callers.
 * Exposes only safe, client-appropriate fields from the Student entity.
 * The JPA entity is never returned directly.
 */
public class StudentResponse {

    private Long id;
    private String registrationNumber;
    private String neopatId;
    private String name;
    private String branch;
    private Integer batch;
    private BigDecimal cgpa;
    private String phoneNumber;
    private Instant createdAt;
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Factory method — converts a JPA entity to a response DTO.
    // Keeps mapping logic close to the DTO, away from the service.
    // -------------------------------------------------------------------------
    public static StudentResponse from(Student student) {
        StudentResponse response = new StudentResponse();
        response.id = student.getId();
        response.registrationNumber = student.getRegistrationNumber();
        response.neopatId = student.getNeopatId();
        response.name = student.getName();
        response.branch = student.getBranch();
        response.batch = student.getBatch();
        response.cgpa = student.getCgpa();
        response.phoneNumber = student.getPhoneNumber();
        response.createdAt = student.getCreatedAt();
        response.updatedAt = student.getUpdatedAt();
        return response;
    }

    // -------------------------------------------------------------------------
    // Getters (read-only response object)
    // -------------------------------------------------------------------------
    public Long getId() { return id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getNeopatId() { return neopatId; }
    public String getName() { return name; }
    public String getBranch() { return branch; }
    public Integer getBatch() { return batch; }
    public BigDecimal getCgpa() { return cgpa; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

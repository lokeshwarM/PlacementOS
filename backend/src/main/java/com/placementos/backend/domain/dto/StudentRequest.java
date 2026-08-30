package com.placementos.backend.domain.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Inbound DTO for creating or updating a Student profile.
 * JPA entity is not exposed directly to API callers.
 *
 * Validation annotations enforce basic field-level constraints.
 * Database uniqueness constraints remain the authoritative guard.
 */
public class StudentRequest {

    @NotBlank(message = "Registration number is required.")
    @Size(max = 20, message = "Registration number must not exceed 20 characters.")
    private String registrationNumber;

    @Size(max = 20, message = "NeoPAT ID must not exceed 20 characters.")
    private String neopatId; // nullable — not all students have a NeoPAT ID

    @NotBlank(message = "Name is required.")
    @Size(max = 255, message = "Name must not exceed 255 characters.")
    private String name;

    @NotBlank(message = "Branch is required.")
    @Size(max = 100, message = "Branch must not exceed 100 characters.")
    private String branch;

    @NotNull(message = "Batch year is required.")
    @Min(value = 2000, message = "Batch year must be 2000 or later.")
    @Max(value = 2100, message = "Batch year must be 2100 or earlier.")
    private Integer batch;

    @NotNull(message = "CGPA is required.")
    @DecimalMin(value = "0.00", message = "CGPA must be at least 0.00.")
    @DecimalMax(value = "10.00", message = "CGPA must not exceed 10.00.")
    private BigDecimal cgpa;

    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber; // optional

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Integer getBatch() { return batch; }
    public void setBatch(Integer batch) { this.batch = batch; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

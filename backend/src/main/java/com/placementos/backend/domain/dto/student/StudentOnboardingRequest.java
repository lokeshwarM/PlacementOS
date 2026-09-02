package com.placementos.backend.domain.dto.student;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StudentOnboardingRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    private String neopatId;

    @NotBlank(message = "Branch is required")
    private String branch;

    @NotNull(message = "Batch is required")
    private Integer batch;

    @NotNull(message = "CGPA is required")
    @DecimalMin(value = "0.00", message = "CGPA cannot be negative")
    @DecimalMax(value = "10.00", message = "CGPA cannot exceed 10.00")
    private BigDecimal cgpa;

    private String phoneNumber;

    @NotBlank(message = "Degree is required")
    private String degree;

    private String specialization;

    private Integer standingArrears;

    private String gender;

    public StudentOnboardingRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Integer getBatch() { return batch; }
    public void setBatch(Integer batch) { this.batch = batch; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Integer getStandingArrears() { return standingArrears; }
    public void setStandingArrears(Integer standingArrears) { this.standingArrears = standingArrears; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

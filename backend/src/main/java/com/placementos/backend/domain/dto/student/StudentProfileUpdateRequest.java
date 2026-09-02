package com.placementos.backend.domain.dto.student;

public class StudentProfileUpdateRequest {

    private String name;
    private String phoneNumber;
    private String specialization;
    private Integer standingArrears;
    private String gender;

    public StudentProfileUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Integer getStandingArrears() { return standingArrears; }
    public void setStandingArrears(Integer standingArrears) { this.standingArrears = standingArrears; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

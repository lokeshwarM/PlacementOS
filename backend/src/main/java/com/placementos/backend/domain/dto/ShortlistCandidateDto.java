package com.placementos.backend.domain.dto;

public class ShortlistCandidateDto {
    private String registrationNumber;
    private String neopatId;
    private String name;
    private String role;
    private String evidence;

    public ShortlistCandidateDto() {}

    public ShortlistCandidateDto(String registrationNumber, String neopatId, String name, String role, String evidence) {
        this.registrationNumber = registrationNumber;
        this.neopatId = neopatId;
        this.name = name;
        this.role = role;
        this.evidence = evidence;
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getNeopatId() { return neopatId; }
    public void setNeopatId(String neopatId) { this.neopatId = neopatId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}

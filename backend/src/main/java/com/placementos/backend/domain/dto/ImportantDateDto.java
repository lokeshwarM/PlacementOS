package com.placementos.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportantDateDto {

    private String description;
    private Instant date;
    private String rawText;

    public ImportantDateDto() {}

    public ImportantDateDto(String description, Instant date, String rawText) {
        this.description = description;
        this.date = date;
        this.rawText = rawText;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
}

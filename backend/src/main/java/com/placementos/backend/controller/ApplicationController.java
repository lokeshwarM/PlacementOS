package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.ApplicationRequest;
import com.placementos.backend.domain.dto.ApplicationResponse;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Application operations.
 * Minimal endpoints as requested.
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.createApplication(request.getStudentId(), request.getPlacementDriveId());
        return new ResponseEntity<>(ApplicationResponse.from(application), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long id) {
        return applicationService.findById(id)
                .map(ApplicationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApplicationResponse> getApplicationByStudentAndDrive(
            @RequestParam Long studentId,
            @RequestParam Long placementDriveId) {
        return applicationService.findByStudentAndDrive(studentId, placementDriveId)
                .map(ApplicationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.PlacementDriveRequest;
import com.placementos.backend.domain.dto.PlacementDriveResponse;
import com.placementos.backend.domain.service.PlacementDriveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Placement Drive operations.
 * Handles HTTP concerns: mapping, validation, status codes, and routing to the service layer.
 */
@RestController
@RequestMapping("/api/v1/placements")
public class PlacementDriveController {

    private final PlacementDriveService placementDriveService;

    public PlacementDriveController(PlacementDriveService placementDriveService) {
        this.placementDriveService = placementDriveService;
    }

    @PostMapping
    public ResponseEntity<PlacementDriveResponse> createPlacementDrive(@Valid @RequestBody PlacementDriveRequest request) {
        PlacementDriveResponse response = placementDriveService.createPlacementDrive(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PlacementDriveResponse>> getAllPlacementDrives() {
        List<PlacementDriveResponse> responses = placementDriveService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlacementDriveResponse> getPlacementDriveById(@PathVariable Long id) {
        PlacementDriveResponse response = placementDriveService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/source-email/{sourceEmailId}")
    public ResponseEntity<PlacementDriveResponse> getPlacementDriveBySourceEmailId(@PathVariable String sourceEmailId) {
        return placementDriveService.findBySourceEmailId(sourceEmailId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlacementDriveResponse> updatePlacementDrive(
            @PathVariable Long id,
            @Valid @RequestBody PlacementDriveRequest request) {
        PlacementDriveResponse response = placementDriveService.updatePlacementDrive(id, request);
        return ResponseEntity.ok(response);
    }
}

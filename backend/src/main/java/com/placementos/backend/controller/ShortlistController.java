package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.ShortlistResponse;
import com.placementos.backend.domain.service.ShortlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Shortlist queries.
 */
@RestController
@RequestMapping("/api/v1")
public class ShortlistController {

    private final ShortlistService shortlistService;

    public ShortlistController(ShortlistService shortlistService) {
        this.shortlistService = shortlistService;
    }

    @GetMapping("/placements/{placementId}/shortlists")
    public ResponseEntity<List<ShortlistResponse>> getShortlistsByPlacement(@PathVariable Long placementId) {
        List<ShortlistResponse> responses = shortlistService.findByPlacementDriveId(placementId)
                .stream()
                .map(ShortlistResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shortlists/registration/{registrationNumber}")
    public ResponseEntity<List<ShortlistResponse>> getShortlistsByRegistration(@PathVariable String registrationNumber) {
        List<ShortlistResponse> responses = shortlistService.findByRegistrationNumber(registrationNumber)
                .stream()
                .map(ShortlistResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shortlists/neopat/{neopatId}")
    public ResponseEntity<List<ShortlistResponse>> getShortlistsByNeopatId(@PathVariable String neopatId) {
        List<ShortlistResponse> responses = shortlistService.findByNeopatId(neopatId)
                .stream()
                .map(ShortlistResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shortlists/name/{name}")
    public ResponseEntity<List<ShortlistResponse>> getShortlistsByName(@PathVariable String name) {
        List<ShortlistResponse> responses = shortlistService.findByCandidateName(name)
                .stream()
                .map(ShortlistResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}

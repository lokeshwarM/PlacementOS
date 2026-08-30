package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.PlacementDriveRequest;
import com.placementos.backend.domain.dto.PlacementDriveResponse;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.enums.DriveStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business service for placement drive operations.
 *
 * No eligibility evaluation or shortlist processing is implemented here.
 * eligibility_criteria is stored as JSONB data only.
 *
 * Transaction strategy:
 *  - @Transactional on writes.
 *  - Reads are not transactional unless consistency with related data is needed.
 */
@Service
@Transactional(readOnly = true)
public class PlacementDriveService {

    private final PlacementDriveRepository placementDriveRepository;

    public PlacementDriveService(PlacementDriveRepository placementDriveRepository) {
        this.placementDriveRepository = placementDriveRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    @Transactional
    public PlacementDriveResponse createPlacementDrive(PlacementDriveRequest request) {
        PlacementDrive drive = new PlacementDrive();
        applyRequest(drive, request);
        return PlacementDriveResponse.from(placementDriveRepository.save(drive));
    }

    @Transactional
    public PlacementDriveResponse updatePlacementDrive(Long id, PlacementDriveRequest request) {
        PlacementDrive drive = placementDriveRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(id));
        applyRequest(drive, request);
        return PlacementDriveResponse.from(placementDriveRepository.save(drive));
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public PlacementDriveResponse findById(Long id) {
        return placementDriveRepository.findById(id)
                .map(PlacementDriveResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(id));
    }

    public List<PlacementDriveResponse> findAll() {
        return placementDriveRepository.findAll()
                .stream()
                .map(PlacementDriveResponse::from)
                .toList();
    }

    public List<PlacementDriveResponse> findByStatus(DriveStatus status) {
        return placementDriveRepository.findByStatus(status)
                .stream()
                .map(PlacementDriveResponse::from)
                .toList();
    }

    /**
     * Looks up a placement drive by the Gmail Message-ID of the source email.
     * Used during ingestion to check whether this email already produced a drive record.
     */
    public Optional<PlacementDriveResponse> findBySourceEmailId(String sourceEmailId) {
        return placementDriveRepository.findAll()
                .stream()
                .filter(d -> sourceEmailId.equals(d.getSourceEmailId()))
                .findFirst()
                .map(PlacementDriveResponse::from);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void applyRequest(PlacementDrive drive, PlacementDriveRequest request) {
        drive.setCompanyName(request.getCompanyName());
        drive.setTitle(request.getTitle());
        drive.setDescription(request.getDescription());
        drive.setReceivedAt(request.getReceivedAt());
        drive.setApplicationDeadline(request.getApplicationDeadline());
        drive.setSourceEmailId(request.getSourceEmailId());
        drive.setEligibilityCriteria(request.getEligibilityCriteria());
        if (request.getStatus() != null) {
            drive.setStatus(request.getStatus());
        }
    }
}

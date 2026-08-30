package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.PlacementDriveRequest;
import com.placementos.backend.domain.dto.PlacementDriveResponse;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.enums.DriveStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link PlacementDriveService}.
 */
@ExtendWith(MockitoExtension.class)
class PlacementDriveServiceTest {

    @Mock
    PlacementDriveRepository placementDriveRepository;

    @InjectMocks
    PlacementDriveService placementDriveService;

    @Test
    void createPlacementDrive_success() {
        PlacementDriveRequest request = new PlacementDriveRequest();
        request.setCompanyName("Google");

        PlacementDrive saved = buildSavedDrive(1L, "Google", "MSG-001");
        when(placementDriveRepository.save(any(PlacementDrive.class))).thenReturn(saved);

        PlacementDriveResponse response = placementDriveService.createPlacementDrive(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("Google");
    }

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(placementDriveRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placementDriveService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void findBySourceEmailId_found() {
        PlacementDrive drive = buildSavedDrive(1L, "Microsoft", "MSG-XYZ");
        when(placementDriveRepository.findBySourceEmailId("MSG-XYZ")).thenReturn(Optional.of(drive));

        Optional<PlacementDriveResponse> result = placementDriveService.findBySourceEmailId("MSG-XYZ");

        assertThat(result).isPresent();
        assertThat(result.get().getSourceEmailId()).isEqualTo("MSG-XYZ");
    }

    @Test
    void findBySourceEmailId_notFound_returnsEmpty() {
        when(placementDriveRepository.findBySourceEmailId("MISSING")).thenReturn(Optional.empty());

        Optional<PlacementDriveResponse> result = placementDriveService.findBySourceEmailId("MISSING");

        assertThat(result).isEmpty();
    }

    private PlacementDrive buildSavedDrive(Long id, String companyName, String sourceEmailId) {
        try {
            PlacementDrive d = new PlacementDrive();
            var idField = PlacementDrive.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(d, id);
            d.setCompanyName(companyName);
            d.setSourceEmailId(sourceEmailId);
            d.setStatus(DriveStatus.OPEN);
            return d;
        } catch (Exception e) {
            throw new RuntimeException("Test helper failed to build placement drive", e);
        }
    }
}

package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.ClassificationResultDto;
import com.placementos.backend.domain.dto.ExtractedRoleDto;
import com.placementos.backend.domain.dto.StructuredEligibilityDto;
import com.placementos.backend.domain.dto.StructuredPlacementExtractionResult;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.PlacementRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlacementIngestionServiceTest {

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private ProcessedEmailService processedEmailService;

    private PlacementIngestionService service;

    @BeforeEach
    void setUp() {
        service = new PlacementIngestionService(
                placementDriveRepository,
                placementRoleRepository,
                processedEmailService
        );
    }

    @Test
    void ingestExtractionResult_nonPlacement_marksStatusAndDoesNotCreateDrive() {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-club-1");
        result.setClassification(new ClassificationResultDto(false, 0.95, List.of("Dance club matched")));

        Optional<PlacementDrive> driveOpt = service.ingestExtractionResult(result);

        assertTrue(driveOpt.isEmpty());
        verify(processedEmailService).markNonPlacement(eq("msg-club-1"), contains("Dance club matched"));
        verifyNoInteractions(placementDriveRepository);
    }

    @Test
    void ingestExtractionResult_missingCompany_rejectsAndMarksFailed() {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-invalid-1");
        result.setClassification(new ClassificationResultDto(true, 0.90, List.of("Placement signal")));
        result.setCompanyName(""); // blank

        Optional<PlacementDrive> driveOpt = service.ingestExtractionResult(result);

        assertTrue(driveOpt.isEmpty());
        verify(processedEmailService).markFailed(eq("msg-invalid-1"), contains("missing company name"));
        verifyNoInteractions(placementDriveRepository);
    }

    @Test
    void ingestExtractionResult_lowConfidence_rejectsAndMarksFailed() {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-low-1");
        result.setClassification(new ClassificationResultDto(true, 0.35, List.of("Uncertain signal")));
        result.setCompanyName("Some Company");

        Optional<PlacementDrive> driveOpt = service.ingestExtractionResult(result);

        assertTrue(driveOpt.isEmpty());
        verify(processedEmailService).markFailed(eq("msg-low-1"), contains("Low confidence"));
        verifyNoInteractions(placementDriveRepository);
    }

    @Test
    void ingestExtractionResult_validMultiRole_persistsDriveAndRoles() {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-msft-1");
        result.setClassification(new ClassificationResultDto(true, 0.95, List.of("Placement drive signal")));
        result.setCompanyName("Microsoft");
        result.setDriveTitle("Microsoft Campus Hiring 2027");
        result.setApplicationDeadline(Instant.now().plusSeconds(86400));

        StructuredEligibilityDto common = new StructuredEligibilityDto();
        common.setMinimumCgpa(7.5);
        common.setAllowedBatches(List.of("2027"));
        result.setCommonEligibility(common);

        ExtractedRoleDto role1 = new ExtractedRoleDto("Software Engineer", 1);
        StructuredEligibilityDto role1Elig = new StructuredEligibilityDto();
        role1Elig.setAllowedBranches(List.of("CSE", "IT"));
        role1.setEligibility(role1Elig);

        ExtractedRoleDto role2 = new ExtractedRoleDto("Data Analyst", 2);
        StructuredEligibilityDto role2Elig = new StructuredEligibilityDto();
        role2Elig.setMinimumCgpa(8.0);
        role2.setEligibility(role2Elig);

        result.setRoles(List.of(role1, role2));

        when(placementDriveRepository.findBySourceEmailId("msg-msft-1")).thenReturn(Optional.empty());
        when(placementDriveRepository.saveAndFlush(any(PlacementDrive.class))).thenAnswer(inv -> {
            PlacementDrive d = inv.getArgument(0);
            d.setId(101L);
            return d;
        });
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(101L)).thenReturn(new ArrayList<>());

        Optional<PlacementDrive> driveOpt = service.ingestExtractionResult(result);

        assertTrue(driveOpt.isPresent());
        PlacementDrive drive = driveOpt.get();
        assertEquals("Microsoft", drive.getCompanyName());
        assertEquals("Microsoft Campus Hiring 2027", drive.getTitle());
        assertNotNull(drive.getEligibilityCriteria());
        assertEquals(7.5, drive.getEligibilityCriteria().get("minimumCgpa"));

        // Verify roles saved
        verify(placementRoleRepository, times(2)).save(any(PlacementRole.class));
        verify(processedEmailService).markExtracted("msg-msft-1");
    }

    @Test
    void ingestExtractionResult_idempotentRetry_updatesExistingDriveAndSynchronizesRoles() {
        PlacementDrive existingDrive = new PlacementDrive();
        existingDrive.setId(202L);
        existingDrive.setCompanyName("Old Name");
        existingDrive.setSourceEmailId("msg-retry-1");

        PlacementRole oldRole = new PlacementRole("Software Engineer", 1);
        oldRole.setId(501L);
        oldRole.setPlacementDrive(existingDrive);
        existingDrive.addRole(oldRole);

        when(placementDriveRepository.findBySourceEmailId("msg-retry-1")).thenReturn(Optional.of(existingDrive));
        when(placementDriveRepository.saveAndFlush(existingDrive)).thenReturn(existingDrive);
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(202L)).thenReturn(List.of(oldRole));

        StructuredPlacementExtractionResult updatedResult = new StructuredPlacementExtractionResult();
        updatedResult.setMessageId("msg-retry-1");
        updatedResult.setClassification(new ClassificationResultDto(true, 0.98, List.of("Signal")));
        updatedResult.setCompanyName("Updated Microsoft");
        updatedResult.setDriveTitle("Updated Title");

        ExtractedRoleDto updatedRole1 = new ExtractedRoleDto("Software Engineer", 1);
        updatedRole1.setDescription("Updated SWE description");
        updatedResult.setRoles(List.of(updatedRole1));

        Optional<PlacementDrive> driveOpt = service.ingestExtractionResult(updatedResult);

        assertTrue(driveOpt.isPresent());
        assertEquals("Updated Microsoft", existingDrive.getCompanyName());
        assertEquals("Updated Title", existingDrive.getTitle());

        // Verify existing role updated in-place without deletion
        verify(placementRoleRepository, never()).delete(any());
        verify(placementRoleRepository).save(oldRole);
        assertEquals("Updated SWE description", oldRole.getRoleDescription());
        verify(processedEmailService).markExtracted("msg-retry-1");
    }
}

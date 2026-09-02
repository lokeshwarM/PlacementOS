package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationStateReconciliationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private StudentEligibilityResultRepository eligibilityResultRepository;

    @Mock
    private ShortlistEntryRepository shortlistEntryRepository;

    private ApplicationStateReconciliationService reconciliationService;

    private Student student;
    private PlacementDrive drive;
    private PlacementRole role1;

    @BeforeEach
    void setUp() {
        reconciliationService = new ApplicationStateReconciliationService(
                applicationRepository,
                studentRepository,
                placementDriveRepository,
                placementRoleRepository,
                eligibilityResultRepository,
                shortlistEntryRepository
        );

        student = new Student();
        student.setId(1L);
        student.setName("Alice Smith");

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setCompanyName("Google");

        role1 = new PlacementRole("Software Engineer", 1);
        role1.setId(100L);
        role1.setPlacementDrive(drive);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(placementDriveRepository.findById(10L)).thenReturn(Optional.of(drive));
        lenient().when(applicationRepository.save(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void eligibleStudent_reconcilesToEligible() {
        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.empty());
        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(10L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of());

        StudentEligibilityResult eligResult = new StudentEligibilityResult(
                student, drive, role1, EligibilityDecision.ELIGIBLE, List.of(), "v1"
        );
        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(List.of(eligResult));

        Application app = reconciliationService.reconcileStudentDriveApplication(1L, 10L, null);

        assertNotNull(app);
        assertEquals(ApplicationStatus.ELIGIBLE, app.getStatus());
        assertEquals(role1, app.getPlacementRole());
    }

    @Test
    void notEligibleStudent_reconcilesToNotEligible() {
        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.empty());
        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(10L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of());

        StudentEligibilityResult notEligResult = new StudentEligibilityResult(
                student, drive, role1, EligibilityDecision.NOT_ELIGIBLE, List.of(), "v1"
        );
        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(List.of(notEligResult));

        Application app = reconciliationService.reconcileStudentDriveApplication(1L, 10L, null);

        assertNotNull(app);
        assertEquals(ApplicationStatus.NOT_ELIGIBLE, app.getStatus());
    }

    @Test
    void shortlistedStudent_reconcilesToShortlisted() {
        Application existing = new Application();
        existing.setStudent(student);
        existing.setPlacementDrive(drive);
        existing.setStatus(ApplicationStatus.APPLIED);

        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.of(existing));

        ShortlistEntry entry = new ShortlistEntry();
        entry.setStudent(student);
        entry.setPlacementDrive(drive);
        entry.setMatchStatus(ShortlistMatchStatus.MATCHED);

        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(10L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of(entry));

        Application app = reconciliationService.reconcileStudentDriveApplication(1L, 10L, null);

        assertEquals(ApplicationStatus.SHORTLISTED, app.getStatus());
    }

    @Test
    void alreadyAppliedStudent_doesNotRegressBackToEligible() {
        Application existing = new Application();
        existing.setStudent(student);
        existing.setPlacementDrive(drive);
        existing.setStatus(ApplicationStatus.APPLIED);

        when(applicationRepository.findByStudentIdAndPlacementDriveId(1L, 10L))
                .thenReturn(Optional.of(existing));
        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(10L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of());

        Application app = reconciliationService.reconcileStudentDriveApplication(1L, 10L, null);

        assertEquals(ApplicationStatus.APPLIED, app.getStatus());
        verifyNoInteractions(eligibilityResultRepository);
    }
}

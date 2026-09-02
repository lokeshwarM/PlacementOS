package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.student.*;
import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.*;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPortalServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    @Mock
    private StudentEligibilityResultRepository eligibilityResultRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ShortlistEntryRepository shortlistEntryRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReminderTaskRepository reminderTaskRepository;

    @InjectMocks
    private StudentPortalService studentPortalService;

    private UserAccount user;
    private Student student;
    private PlacementDrive drive;
    private PlacementRole roleSWE;
    private PlacementRole roleData;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(10L);
        student.setName("Alice");
        student.setRegistrationNumber("21BCE1001");
        student.setBranch("CSE");
        student.setBatch(2026);
        student.setCgpa(new BigDecimal("8.50"));
        student.setDegree("B.Tech");

        user = new UserAccount("alice@example.com", "hash", UserRole.STUDENT);
        user.setId(1L);
        user.setStudent(student);
        user.setProfileStatus(ProfileStatus.COMPLETE);

        drive = new PlacementDrive();
        drive.setId(100L);
        drive.setCompanyName("Acme Corp");
        drive.setTitle("Campus Hiring 2026");
        drive.setApplicationDeadline(Instant.now().plus(5, ChronoUnit.DAYS));

        roleSWE = new PlacementRole();
        roleSWE.setId(201L);
        roleSWE.setPlacementDrive(drive);
        roleSWE.setRoleTitle("Software Engineer");
        roleSWE.setEligibilityCriteria(Map.of("min_cgpa", "7.50", "eligible_branches", List.of("CSE", "IT")));

        roleData = new PlacementRole();
        roleData.setId(202L);
        roleData.setPlacementDrive(drive);
        roleData.setRoleTitle("Data Analyst");
        roleData.setEligibilityCriteria(Map.of("min_cgpa", "9.00", "eligible_branches", List.of("CSE")));
    }

    @Test
    @DisplayName("onboardStudent creates new Student and marks profile COMPLETE when all fields valid")
    void onboardStudent_newStudent_success() {
        UserAccount unlinkedUser = new UserAccount("bob@example.com", "hash", UserRole.STUDENT);
        unlinkedUser.setId(2L);

        StudentOnboardingRequest req = new StudentOnboardingRequest();
        req.setName("Bob");
        req.setRegistrationNumber("21BCE1002");
        req.setBranch("ECE");
        req.setBatch(2026);
        req.setCgpa(new BigDecimal("8.20"));
        req.setDegree("B.Tech");

        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(unlinkedUser));
        when(studentRepository.findByRegistrationNumber("21BCE1002")).thenReturn(Optional.empty());

        Student createdStudent = new Student();
        createdStudent.setId(20L);
        createdStudent.setName("Bob");
        createdStudent.setRegistrationNumber("21BCE1002");
        createdStudent.setBranch("ECE");
        createdStudent.setBatch(2026);
        createdStudent.setCgpa(new BigDecimal("8.20"));
        createdStudent.setDegree("B.Tech");

        when(studentRepository.save(any(Student.class))).thenReturn(createdStudent);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(unlinkedUser);

        StudentProfileResponse res = studentPortalService.onboardStudent(2L, req);

        assertThat(res).isNotNull();
        assertThat(unlinkedUser.getProfileStatus()).isEqualTo(ProfileStatus.COMPLETE);
        assertThat(unlinkedUser.getStudent()).isEqualTo(createdStudent);
    }

    @Test
    @DisplayName("onboardStudent throws DuplicateResourceException if registration number is linked to another user")
    void onboardStudent_alreadyLinkedConflict() {
        UserAccount userBob = new UserAccount("bob@example.com", "hash", UserRole.STUDENT);
        userBob.setId(2L);

        UserAccount userCharlie = new UserAccount("charlie@example.com", "hash", UserRole.STUDENT);
        userCharlie.setId(3L);

        StudentOnboardingRequest req = new StudentOnboardingRequest();
        req.setName("Bob");
        req.setRegistrationNumber("21BCE1001");
        req.setBranch("CSE");
        req.setBatch(2026);
        req.setCgpa(new BigDecimal("8.50"));
        req.setDegree("B.Tech");

        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(userBob));
        when(studentRepository.findByRegistrationNumber("21BCE1001")).thenReturn(Optional.of(student));
        when(userAccountRepository.findByStudentId(10L)).thenReturn(Optional.of(userCharlie));

        assertThatThrownBy(() -> studentPortalService.onboardStudent(2L, req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already linked to another account");
    }

    @Test
    @DisplayName("getStudentPlacements maps role-specific eligibility with explainable criteria reasons")
    void getStudentPlacements_roleSpecificEligibility() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PlacementDrive> drivesPage = new PageImpl<>(List.of(drive), pageable, 1);

        when(placementDriveRepository.findAll(pageable)).thenReturn(drivesPage);
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(100L)).thenReturn(List.of(roleSWE, roleData));

        StudentEligibilityResult eligSWE = new StudentEligibilityResult(
                student, drive, roleSWE, EligibilityDecision.ELIGIBLE,
                List.of(Map.of("criterion", "CGPA", "status", "PASS", "reason", "CGPA 8.50 satisfies min 7.50")),
                "v1"
        );
        StudentEligibilityResult eligData = new StudentEligibilityResult(
                student, drive, roleData, EligibilityDecision.NOT_ELIGIBLE,
                List.of(Map.of("criterion", "CGPA", "status", "FAIL", "reason", "CGPA 8.50 below min 9.00")),
                "v1"
        );

        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(10L, 100L))
                .thenReturn(List.of(eligSWE, eligData));
        when(applicationRepository.findByStudentIdAndPlacementDriveId(10L, 100L))
                .thenReturn(Optional.empty());
        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(100L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of());

        PageResponse<StudentPlacementDriveCardResponse> res = studentPortalService.getStudentPlacements(10L, pageable);

        assertThat(res.content()).hasSize(1);
        StudentPlacementDriveCardResponse card = res.content().get(0);

        assertThat(card.companyName()).isEqualTo("Acme Corp");
        assertThat(card.overallEligibility()).isEqualTo(EligibilityDecision.ELIGIBLE);
        assertThat(card.applicationStatus()).isEqualTo(ApplicationStatus.NOT_STARTED);
        assertThat(card.isActionable()).isTrue();
        assertThat(card.roles()).hasSize(2);

        RoleEligibilityCardResponse sweCard = card.roles().stream()
                .filter(r -> r.roleTitle().equals("Software Engineer")).findFirst().orElseThrow();
        assertThat(sweCard.decision()).isEqualTo(EligibilityDecision.ELIGIBLE);
        assertThat(sweCard.criteriaExplanations().get(0)).contains("CGPA 8.50 satisfies min 7.50");

        RoleEligibilityCardResponse dataCard = card.roles().stream()
                .filter(r -> r.roleTitle().equals("Data Analyst")).findFirst().orElseThrow();
        assertThat(dataCard.decision()).isEqualTo(EligibilityDecision.NOT_ELIGIBLE);
        assertThat(dataCard.criteriaExplanations().get(0)).contains("CGPA 8.50 below min 9.00");
    }

    @Test
    @DisplayName("getStudentPlacementDetail maps shortlist status when student is shortlisted")
    void getStudentPlacementDetail_shortlisted() {
        when(placementDriveRepository.findById(100L)).thenReturn(Optional.of(drive));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(100L)).thenReturn(List.of(roleSWE));
        when(eligibilityResultRepository.findByStudentIdAndPlacementDriveId(10L, 100L)).thenReturn(List.of());

        Application app = new Application();
        app.setId(555L);
        app.setStudent(student);
        app.setPlacementDrive(drive);
        app.setStatus(ApplicationStatus.APPLIED);
        when(applicationRepository.findByStudentIdAndPlacementDriveId(10L, 100L)).thenReturn(Optional.of(app));

        ShortlistEntry entry = new ShortlistEntry();
        entry.setStudent(student);
        entry.setMatchStatus(ShortlistMatchStatus.MATCHED);
        entry.setCandidateName("Alice");
        when(shortlistEntryRepository.findByPlacementDriveIdAndMatchStatus(100L, ShortlistMatchStatus.MATCHED))
                .thenReturn(List.of(entry));

        StudentPlacementDetailResponse detail = studentPortalService.getStudentPlacementDetail(10L, 100L);

        assertThat(detail.applicationId()).isEqualTo(555L);
        assertThat(detail.shortlistStatus()).isEqualTo(ShortlistMatchStatus.MATCHED);
        assertThat(detail.shortlistCandidateName()).isEqualTo("Alice");
        assertThat(detail.actionRequired()).contains("Shortlisted");
    }
}

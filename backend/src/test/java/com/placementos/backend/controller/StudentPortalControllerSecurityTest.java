package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.dto.student.StudentProfileResponse;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.ReminderTask;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.enums.ProfileStatus;
import com.placementos.backend.domain.enums.UserRole;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ReminderService;
import com.placementos.backend.domain.service.StudentPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StudentPortalControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentPortalService studentPortalService;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private ReminderService reminderService;

    @MockitoBean
    private AuthenticatedStudentProvider studentProvider;

    @MockitoBean
    private com.placementos.backend.domain.service.TelegramLinkingService telegramLinkingService;

    private MockMvc mockMvc;
    private Student studentAlice;
    private Student studentBob;
    private UserAccount userAlice;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        studentAlice = new Student();
        studentAlice.setId(101L);
        studentAlice.setName("Alice");
        studentAlice.setRegistrationNumber("21BCE1001");

        studentBob = new Student();
        studentBob.setId(102L);
        studentBob.setName("Bob");
        studentBob.setRegistrationNumber("21BCE1002");

        userAlice = new UserAccount("alice@example.com", "hash", UserRole.STUDENT);
        userAlice.setId(1L);
        userAlice.setStudent(studentAlice);
        userAlice.setProfileStatus(ProfileStatus.COMPLETE);
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/student/profile is rejected (401 or 403)")
    void unauthenticatedRequest_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Authenticated student can access their own profile derived from principal")
    void authenticatedStudent_accessOwnProfile() throws Exception {
        when(studentProvider.getUserAccountFromPrincipal(any(Principal.class))).thenReturn(userAlice);
        when(studentPortalService.getStudentProfile(1L))
                .thenReturn(StudentProfileResponse.from(userAlice));

        mockMvc.perform(get("/api/v1/student/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Authenticated student cannot read another student's application (403 Forbidden)")
    void cannotReadOtherStudentApplication() throws Exception {
        when(studentProvider.getStudentFromPrincipal(any(Principal.class))).thenReturn(studentAlice);

        Application bobApp = new Application();
        bobApp.setId(999L);
        bobApp.setStudent(studentBob);
        PlacementDrive drive = new PlacementDrive();
        drive.setId(50L);
        drive.setCompanyName("TechCorp");
        bobApp.setPlacementDrive(drive);

        when(applicationService.findById(999L)).thenReturn(Optional.of(bobApp));
        doThrow(new AccessDeniedException("Access denied: You cannot access or modify resources belonging to another student"))
                .when(studentProvider).verifyOwnership(studentAlice, studentBob.getId());

        mockMvc.perform(get("/api/v1/student/applications/999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Authenticated student cannot stop another student's reminder (403 Forbidden)")
    void cannotStopOtherStudentReminder() throws Exception {
        when(studentProvider.getStudentFromPrincipal(any(Principal.class))).thenReturn(studentAlice);

        ReminderTask bobReminder = new ReminderTask();
        bobReminder.setId(777L);
        bobReminder.setStudent(studentBob);
        PlacementDrive drive = new PlacementDrive();
        drive.setId(50L);
        bobReminder.setPlacementDrive(drive);

        when(reminderService.findById(777L)).thenReturn(Optional.of(bobReminder));
        doThrow(new AccessDeniedException("Access denied"))
                .when(studentProvider).verifyOwnership(studentAlice, studentBob.getId());

        mockMvc.perform(post("/api/v1/student/reminders/777/stop"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Explicit apply marks application APPLIED for authenticated student")
    void explicitApply_success() throws Exception {
        when(studentProvider.getStudentFromPrincipal(any(Principal.class))).thenReturn(studentAlice);

        Application app = new Application();
        app.setId(123L);
        app.setStudent(studentAlice);
        app.setStatus(ApplicationStatus.APPLIED);

        when(applicationService.apply(101L, 123L)).thenReturn(app);

        mockMvc.perform(post("/api/v1/student/applications/123/apply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(123))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/student/telegram/link-token is rejected")
    void unauthenticatedTelegramLink_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/student/telegram/link-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Authenticated student can request a Telegram link token")
    void authenticatedStudent_canRequestLinkToken() throws Exception {
        when(studentProvider.getStudentFromPrincipal(any(Principal.class))).thenReturn(studentAlice);
        when(telegramLinkingService.createLinkToken(studentAlice))
                .thenReturn(new com.placementos.backend.domain.dto.student.TelegramLinkResponse(
                        "abc123token",
                        "https://t.me/PlacementOS_bot?start=abc123token",
                        java.time.Instant.now().plusSeconds(900)
                ));

        mockMvc.perform(post("/api/v1/student/telegram/link-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123token"))
                .andExpect(jsonPath("$.deepLink").value("https://t.me/PlacementOS_bot?start=abc123token"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"STUDENT"})
    @DisplayName("Authenticated student can fetch Telegram connection status")
    void authenticatedStudent_canGetTelegramStatus() throws Exception {
        when(studentProvider.getStudentFromPrincipal(any(Principal.class))).thenReturn(studentAlice);
        when(telegramLinkingService.getStatus(studentAlice))
                .thenReturn(new com.placementos.backend.domain.dto.student.TelegramStatusResponse(
                        true,
                        "alice_tg",
                        java.time.Instant.now()
                ));

        mockMvc.perform(get("/api/v1/student/telegram/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.telegramUsername").value("alice_tg"));
    }
}


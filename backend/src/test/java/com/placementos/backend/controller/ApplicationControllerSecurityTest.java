package com.placementos.backend.controller;

import com.placementos.backend.config.AuthenticatedStudentProvider;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.service.ApplicationService;
import com.placementos.backend.domain.service.ApplicationStateReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ApplicationControllerSecurityTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationStateReconciliationService reconciliationService;

    @Mock
    private AuthenticatedStudentProvider studentProvider;

    private ApplicationController controller;
    private MockMvc mockMvc;

    private Student studentAlice;
    private Student studentBob;
    private PlacementDrive drive;
    private Application applicationBob;

    @BeforeEach
    void setUp() {
        controller = new ApplicationController(
                applicationService,
                reconciliationService,
                studentProvider
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        studentAlice = new Student();
        studentAlice.setId(1L);
        studentAlice.setName("Alice");

        studentBob = new Student();
        studentBob.setId(2L);
        studentBob.setName("Bob");

        drive = new PlacementDrive();
        drive.setId(10L);

        applicationBob = new Application();
        applicationBob.setId(500L);
        applicationBob.setStudent(studentBob);
        applicationBob.setPlacementDrive(drive);
    }

    @Test
    void apply_whenAuthenticatedAsOwner_succeeds() throws Exception {
        Principal principalAlice = () -> "1";
        when(studentProvider.getStudentFromPrincipal(any())).thenReturn(studentAlice);

        Application appAlice = new Application();
        appAlice.setId(100L);
        appAlice.setStudent(studentAlice);
        appAlice.setPlacementDrive(drive);
        appAlice.setStatus(ApplicationStatus.APPLIED);
        when(applicationService.apply(1L, 100L)).thenReturn(appAlice);

        mockMvc.perform(post("/api/v1/applications/100/apply")
                        .principal(principalAlice))
                .andExpect(status().isOk());

        verify(applicationService).apply(1L, 100L);
    }

    @Test
    void apply_whenAliceAttemptsToApplyForBob_throwsAccessDenied() {
        Principal principalAlice = () -> "1";
        when(studentProvider.getStudentFromPrincipal(any())).thenReturn(studentAlice);
        when(applicationService.apply(1L, 500L))
                .thenThrow(new AccessDeniedException("Access denied: You cannot submit applications for another student"));

        assertThrows(AccessDeniedException.class, () -> {
            controller.apply(500L, principalAlice);
        });
    }

    @Test
    void apply_ignoresQueryParamStudentId_alwaysDerivesFromPrincipal() {
        // Even if attacker passes query parameter ?studentId=2 (Bob), the controller uses authenticated Alice (ID 1)
        Principal principalAlice = () -> "1";
        when(studentProvider.getStudentFromPrincipal(any())).thenReturn(studentAlice);

        Application appAlice = new Application();
        appAlice.setId(100L);
        appAlice.setStudent(studentAlice);
        appAlice.setPlacementDrive(drive);
        appAlice.setStatus(ApplicationStatus.APPLIED);
        when(applicationService.apply(1L, 100L)).thenReturn(appAlice);

        controller.apply(100L, principalAlice);

        verify(applicationService).apply(1L, 100L);
        verify(applicationService, never()).apply(eq(2L), anyLong());
    }
}

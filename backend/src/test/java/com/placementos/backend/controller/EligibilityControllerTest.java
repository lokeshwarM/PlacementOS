package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.CriterionEvaluationResult;
import com.placementos.backend.domain.dto.DriveEligibilityEvaluationResponse;
import com.placementos.backend.domain.dto.RoleEligibilityEvaluationResponse;
import com.placementos.backend.domain.enums.CriterionEvaluationStatus;
import com.placementos.backend.domain.enums.CriterionScope;
import com.placementos.backend.domain.enums.CriterionType;
import com.placementos.backend.domain.enums.EligibilityDecision;
import com.placementos.backend.domain.service.EligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class EligibilityControllerTest {

    @Mock
    private EligibilityService eligibilityService;

    @InjectMocks
    private EligibilityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void evaluateForDrive_returnsOkWithRoles() throws Exception {
        DriveEligibilityEvaluationResponse resp = new DriveEligibilityEvaluationResponse(1L, 10L, "Google", "Campus 2027");
        RoleEligibilityEvaluationResponse roleResp = new RoleEligibilityEvaluationResponse();
        roleResp.setStudentId(1L);
        roleResp.setPlacementDriveId(10L);
        roleResp.setPlacementRoleId(100L);
        roleResp.setRoleTitle("Software Engineer");
        roleResp.setDecision(EligibilityDecision.ELIGIBLE);
        roleResp.setEvaluatorVersion("eligibility-v1");
        roleResp.setEvaluatedAt(Instant.now());
        roleResp.setCriteriaResults(List.of(
                new CriterionEvaluationResult(CriterionType.MINIMUM_CGPA, CriterionEvaluationStatus.PASS, CriterionScope.DRIVE_COMMON, 7.5, 8.5, "Passed")
        ));
        resp.setRoleResults(List.of(roleResp));

        when(eligibilityService.evaluateAndPersistForDrive(1L, 10L)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/eligibility/students/1/drives/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.placementDriveId").value(10))
                .andExpect(jsonPath("$.companyName").value("Google"))
                .andExpect(jsonPath("$.roleResults[0].roleTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.roleResults[0].decision").value("ELIGIBLE"))
                .andExpect(jsonPath("$.roleResults[0].evaluatorVersion").value("eligibility-v1"));

        verify(eligibilityService).evaluateAndPersistForDrive(1L, 10L);
    }

    @Test
    void getPersistedForDrive_returnsOk() throws Exception {
        DriveEligibilityEvaluationResponse resp = new DriveEligibilityEvaluationResponse(1L, 10L, "Google", "Campus 2027");
        when(eligibilityService.getPersistedResultsForDrive(1L, 10L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/eligibility/students/1/drives/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.placementDriveId").value(10));
    }

    @Test
    void evaluateForRole_returnsOk() throws Exception {
        RoleEligibilityEvaluationResponse roleResp = new RoleEligibilityEvaluationResponse();
        roleResp.setStudentId(1L);
        roleResp.setPlacementDriveId(10L);
        roleResp.setPlacementRoleId(100L);
        roleResp.setRoleTitle("Data Scientist");
        roleResp.setDecision(EligibilityDecision.REVIEW_REQUIRED);

        when(eligibilityService.evaluateAndPersistForRole(1L, 100L)).thenReturn(roleResp);

        mockMvc.perform(post("/api/v1/eligibility/students/1/roles/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementRoleId").value(100))
                .andExpect(jsonPath("$.decision").value("REVIEW_REQUIRED"));
    }
}

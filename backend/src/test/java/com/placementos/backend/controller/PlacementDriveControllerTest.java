package com.placementos.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.domain.dto.PlacementDriveRequest;
import com.placementos.backend.domain.dto.PlacementDriveResponse;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.service.PlacementDriveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlacementDriveControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlacementDriveService placementDriveService;

    @InjectMocks
    private PlacementDriveController placementDriveController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(placementDriveController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPlacementDrive_success_returns201() throws Exception {
        PlacementDriveRequest request = new PlacementDriveRequest();
        request.setCompanyName("Google");

        PlacementDriveResponse response = new PlacementDriveResponse();
        when(placementDriveService.createPlacementDrive(any(PlacementDriveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/placements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getPlacementDriveById_success_returns200() throws Exception {
        PlacementDriveResponse response = new PlacementDriveResponse();
        when(placementDriveService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/placements/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPlacementDriveById_missing_returns404() throws Exception {
        when(placementDriveService.findById(999L)).thenThrow(ResourceNotFoundException.placementDrive(999L));

        mockMvc.perform(get("/api/v1/placements/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

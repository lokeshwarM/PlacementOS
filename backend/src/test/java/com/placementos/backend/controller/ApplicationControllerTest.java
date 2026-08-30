package com.placementos.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.domain.dto.ApplicationRequest;
import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController applicationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createApplication_success_returns201() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(1L);
        request.setPlacementDriveId(2L);

        Application application = new Application();
        try {
            var idField = Application.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(application, 100L);
            
            Student s = new Student();
            var sid = Student.class.getDeclaredField("id"); sid.setAccessible(true); sid.set(s, 1L);
            application.setStudent(s);
            
            PlacementDrive pd = new PlacementDrive();
            var pdid = PlacementDrive.class.getDeclaredField("id"); pdid.setAccessible(true); pdid.set(pd, 2L);
            application.setPlacementDrive(pd);
        } catch (Exception e) {}

        when(applicationService.createApplication(1L, 2L)).thenReturn(application);

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createApplication_duplicate_returns409() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(1L);
        request.setPlacementDriveId(2L);

        when(applicationService.createApplication(1L, 2L))
                .thenThrow(DuplicateResourceException.application(1L, 2L));

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

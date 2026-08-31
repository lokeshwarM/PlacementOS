package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.ClassificationResultDto;
import com.placementos.backend.domain.dto.StructuredPlacementExtractionResult;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailMessageRepository;
import com.placementos.backend.domain.service.PlacementIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalExtractionControllerTest {

    @Mock
    private PlacementIngestionService placementIngestionService;

    @Mock
    private GmailMessageRepository gmailMessageRepository;

    @InjectMocks
    private InternalExtractionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void receiveExtractionResult_placementSuccess_returns200() throws Exception {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-ext-101");
        result.setClassification(new ClassificationResultDto(true, 0.95, List.of("Evidence")));
        result.setCompanyName("Google");

        PlacementDrive drive = new PlacementDrive();
        drive.setId(50L);
        drive.setCompanyName("Google");
        drive.addRole(new PlacementRole("Software Engineer", 1));

        when(placementIngestionService.ingestExtractionResult(any(StructuredPlacementExtractionResult.class)))
                .thenReturn(Optional.of(drive));

        mockMvc.perform(post("/api/v1/internal/extraction/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.messageId").value("msg-ext-101"))
                .andExpect(jsonPath("$.driveId").value(50))
                .andExpect(jsonPath("$.companyName").value("Google"))
                .andExpect(jsonPath("$.roleCount").value(1));

        verify(placementIngestionService).ingestExtractionResult(any(StructuredPlacementExtractionResult.class));
    }

    @Test
    void receiveExtractionResult_nonPlacement_returns200Recorded() throws Exception {
        StructuredPlacementExtractionResult result = new StructuredPlacementExtractionResult();
        result.setMessageId("msg-club-102");
        result.setClassification(new ClassificationResultDto(false, 0.90, List.of("Club event")));

        when(placementIngestionService.ingestExtractionResult(any(StructuredPlacementExtractionResult.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/internal/extraction/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.isPlacement").value(false));
    }

    @Test
    void getMessageForProcessing_found_returns200() throws Exception {
        GmailSource source = new GmailSource();
        source.setEmailAddress("cdc@vit.ac.in");

        GmailMessage msg = new GmailMessage();
        msg.setMessageId("msg-proc-1");
        msg.setGmailSource(source);
        msg.setSubject("Amazon Hiring");
        msg.setSender("cdc@vit.ac.in");
        msg.setPlainTextBody("Please find JD details.");
        msg.setSnippet("Please find JD details.");
        msg.setGmailInternalDate(Instant.now());

        when(gmailMessageRepository.findByMessageId("msg-proc-1")).thenReturn(Optional.of(msg));

        mockMvc.perform(get("/api/v1/internal/messages/msg-proc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("msg-proc-1"))
                .andExpect(jsonPath("$.sourceEmail").value("cdc@vit.ac.in"))
                .andExpect(jsonPath("$.subject").value("Amazon Hiring"));
    }

    @Test
    void getMessageForProcessing_notFound_returns404() throws Exception {
        when(gmailMessageRepository.findByMessageId("msg-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/internal/messages/msg-missing"))
                .andExpect(status().isNotFound());
    }
}

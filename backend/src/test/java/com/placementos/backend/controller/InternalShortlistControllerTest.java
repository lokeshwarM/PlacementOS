package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.DocumentProcessingResultDto;
import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.repository.ShortlistEntryRepository;
import com.placementos.backend.domain.service.GmailAttachmentService;
import com.placementos.backend.domain.service.ShortlistMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalShortlistControllerTest {

    @Mock
    private GmailAttachmentService gmailAttachmentService;

    @Mock
    private ShortlistMatchingService shortlistMatchingService;

    @Mock
    private ShortlistEntryRepository shortlistEntryRepository;

    private InternalShortlistController controller;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        controller = new InternalShortlistController(
                gmailAttachmentService,
                shortlistMatchingService,
                shortlistEntryRepository,
                "test-internal-key"
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAttachmentContent_withoutKey_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/internal/attachments/1/content"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAttachmentContent_withValidKey_returnsBytes() throws Exception {
        byte[] expectedBytes = "Mock binary content".getBytes();
        when(gmailAttachmentService.getAttachmentContent(1L)).thenReturn(expectedBytes);

        mockMvc.perform(get("/api/v1/internal/attachments/1/content")
                        .header("X-Internal-Service-Key", "test-internal-key"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedBytes));
    }

    @Test
    void submitShortlistResult_withoutKey_returnsForbidden() throws Exception {
        DocumentProcessingResultDto dto = new DocumentProcessingResultDto();
        dto.setAttachmentId(1L);

        mockMvc.perform(post("/api/v1/internal/shortlists/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitShortlistResult_withValidKey_returnsOk() throws Exception {
        DocumentProcessingResultDto dto = new DocumentProcessingResultDto();
        dto.setAttachmentId(1L);

        when(shortlistMatchingService.processDocumentShortlist(any())).thenReturn(List.of(new ShortlistEntry()));

        mockMvc.perform(post("/api/v1/internal/shortlists/result")
                        .header("X-Internal-Service-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void triggerAttachmentDownload_returnsOk() throws Exception {
        Attachment attachment = new Attachment();
        attachment.setFilename("shortlist.xlsx");

        when(gmailAttachmentService.downloadAndStoreAttachment(1L)).thenReturn(attachment);

        mockMvc.perform(post("/api/v1/internal/attachments/1/process"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnresolvedEntries_returnsOk() throws Exception {
        when(shortlistEntryRepository.findByMatchStatus(ShortlistMatchStatus.AMBIGUOUS))
                .thenReturn(new ArrayList<>());
        when(shortlistEntryRepository.findByMatchStatus(ShortlistMatchStatus.REVIEW_REQUIRED))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/internal/shortlists/unresolved"))
                .andExpect(status().isOk());
    }
}

package com.placementos.backend.controller;

import com.placementos.backend.domain.service.GmailHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalGmailControllerTest {

    @Mock
    private GmailHistoryService gmailHistoryService;

    @InjectMocks
    private InternalGmailController controller;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void triggerHistorySync_validEmail_returns200() throws Exception {
        mockMvc.perform(post("/api/internal/gmail/history/sync")
                .param("email", "cdc@example.com"))
                .andExpect(status().isOk());

        verify(gmailHistoryService).syncHistory("cdc@example.com");
    }

    @Test
    public void triggerHistorySync_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/internal/gmail/history/sync"))
                .andExpect(status().isBadRequest());
    }
}

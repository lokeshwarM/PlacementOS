package com.placementos.backend.controller;

import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.enums.GmailMessageRetrievalStatus;
import com.placementos.backend.domain.service.GmailHistoryService;
import com.placementos.backend.domain.service.GmailMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalGmailControllerTest {

    @Mock
    private GmailHistoryService gmailHistoryService;

    @Mock
    private GmailMessageService gmailMessageService;

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

    @Test
    public void retrieveMessage_validParams_returns200() throws Exception {
        GmailMessage msg = new GmailMessage();
        msg.setId(1L);
        msg.setMessageId("msg-123");
        msg.setRetrievalStatus(GmailMessageRetrievalStatus.RETRIEVED);

        when(gmailMessageService.retrieveAndPersistMessage("cdc@example.com", "msg-123")).thenReturn(msg);

        mockMvc.perform(post("/api/internal/gmail/messages/retrieve")
                .param("email", "cdc@example.com")
                .param("messageId", "msg-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("msg-123"))
                .andExpect(jsonPath("$.status").value("RETRIEVED"));

        verify(gmailMessageService).retrieveAndPersistMessage("cdc@example.com", "msg-123");
    }

    @Test
    public void retrieveMessage_notFound_returns404() throws Exception {
        when(gmailMessageService.retrieveAndPersistMessage("cdc@example.com", "msg-999")).thenReturn(null);

        mockMvc.perform(post("/api/internal/gmail/messages/retrieve")
                .param("email", "cdc@example.com")
                .param("messageId", "msg-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void retrieveMessage_missingParams_returns400() throws Exception {
        mockMvc.perform(post("/api/internal/gmail/messages/retrieve"))
                .andExpect(status().isBadRequest());
    }
}

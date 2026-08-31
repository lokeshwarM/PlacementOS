package com.placementos.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.PubSubJwtValidator;
import com.placementos.backend.domain.service.GmailNotificationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for Pub/Sub push endpoint authentication and envelope handling.
 *
 * Uses standalone MockMvc to avoid full application context startup while still
 * exercising the controller logic. JWT validation is mocked — real JWT signature
 * verification against Google's public keys is an integration concern tested separately.
 */
@ExtendWith(MockitoExtension.class)
public class PubSubAuthenticationTest {

    @Mock
    private PubSubJwtValidator jwtValidator;

    @Mock
    private GmailNotificationHandler notificationHandler;

    @InjectMocks
    private PubSubPushController controller;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        // Standalone MockMvc: no Spring context required
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        // Inject the ObjectMapper Spring Boot 4 provides
        org.springframework.test.util.ReflectionTestUtils.setField(
                controller, "objectMapper", new ObjectMapper());
    }

    private String buildEnvelope(String emailAddress, String historyId) throws Exception {
        String notification = String.format(
                "{\"emailAddress\":\"%s\",\"historyId\":\"%s\"}", emailAddress, historyId);
        String encodedData = Base64.getEncoder().encodeToString(notification.getBytes());
        return String.format(
                "{\"message\":{\"data\":\"%s\",\"messageId\":\"pub-001\",\"publishTime\":\"2026-01-01T00:00:00Z\"},\"subscription\":\"projects/test/subscriptions/gmail\"}",
                encodedData);
    }

    // Test 5: Missing Authorization header → 401
    @Test
    public void push_missingAuthorizationHeader_returns401() throws Exception {
        when(jwtValidator.isValid(null)).thenReturn(false);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "12345")))
                .andExpect(status().isUnauthorized());
    }

    // Test 6: Invalid JWT token → 401
    @Test
    public void push_invalidJwt_returns401() throws Exception {
        when(jwtValidator.isValid("invalid.jwt.token")).thenReturn(false);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer invalid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "12345")))
                .andExpect(status().isUnauthorized());
    }

    // Test 7: Wrong audience in JWT → validator returns false → 401
    @Test
    public void push_wrongAudience_returns401() throws Exception {
        when(jwtValidator.isValid("wrong.audience.jwt")).thenReturn(false);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer wrong.audience.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "12345")))
                .andExpect(status().isUnauthorized());
    }

    // Test 8: Wrong service account in JWT → validator returns false → 401
    @Test
    public void push_wrongServiceAccount_returns401() throws Exception {
        when(jwtValidator.isValid("wrong.sa.jwt")).thenReturn(false);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer wrong.sa.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "12345")))
                .andExpect(status().isUnauthorized());
    }

    // Test 9: Valid JWT with wrong issuer → validator returns false → 401
    @Test
    public void push_wrongIssuer_returns401() throws Exception {
        when(jwtValidator.isValid("wrong.issuer.jwt")).thenReturn(false);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer wrong.issuer.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "12345")))
                .andExpect(status().isUnauthorized());
    }

    // Test: Valid JWT + valid envelope → 204
    @Test
    public void push_validJwt_validEnvelope_returns204() throws Exception {
        when(jwtValidator.isValid("valid.jwt.token")).thenReturn(true);
        when(notificationHandler.handle("cdc@example.com", "99999"))
                .thenReturn(GmailNotificationHandler.HandlerResult.CURSOR_ADVANCED);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer valid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildEnvelope("cdc@example.com", "99999")))
                .andExpect(status().isNoContent());
    }

    // Test: Valid JWT + empty envelope → 400
    @Test
    public void push_validJwt_emptyEnvelope_returns400() throws Exception {
        when(jwtValidator.isValid("valid.jwt.token")).thenReturn(true);

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer valid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // Test: Valid JWT + invalid Base64 data → 400
    @Test
    public void push_validJwt_invalidBase64Data_returns400() throws Exception {
        when(jwtValidator.isValid("valid.jwt.token")).thenReturn(true);

        String envelope = "{\"message\":{\"data\":\"!!invalid-base64!!\",\"messageId\":\"123\"},\"subscription\":\"sub\"}";

        mockMvc.perform(post("/api/internal/gmail/pubsub/push")
                .header("Authorization", "Bearer valid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelope))
                .andExpect(status().isBadRequest());
    }
}

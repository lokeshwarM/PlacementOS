package com.placementos.backend.controller;

import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.service.GmailOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class GmailOAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GmailOAuthService gmailOAuthService;

    @InjectMocks
    private GmailOAuthController gmailOAuthController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gmailOAuthController).build();
    }

    @Test
    public void testAuthorizeRedirect() throws Exception {
        when(gmailOAuthService.generateAuthorizationUrl()).thenReturn("https://accounts.google.com/o/oauth2/auth?client_id=123");

        mockMvc.perform(get("/api/internal/gmail/oauth2/authorize"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://accounts.google.com/o/oauth2/auth?client_id=123"));
    }

    @Test
    public void testCallbackSuccess() throws Exception {
        GmailSource source = new GmailSource();
        source.setEmailAddress("test@cdc.vit.edu");
        
        when(gmailOAuthService.handleCallback("valid-code", "valid-state")).thenReturn(source);

        mockMvc.perform(get("/api/internal/gmail/oauth2/callback")
                .param("code", "valid-code")
                .param("state", "valid-state"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully authenticated and registered Gmail Source: test@cdc.vit.edu"));
    }

    @Test
    public void testCallbackMissingParams() throws Exception {
        mockMvc.perform(get("/api/internal/gmail/oauth2/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing code or state parameter."));
    }

    @Test
    public void testCallbackErrorParam() throws Exception {
        mockMvc.perform(get("/api/internal/gmail/oauth2/callback")
                .param("error", "access_denied"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("OAuth Authorization Error: access_denied"));
    }
    
    @Test
    public void testCallbackInvalidState() throws Exception {
        when(gmailOAuthService.handleCallback(anyString(), anyString())).thenThrow(new IllegalArgumentException("Invalid or expired OAuth state parameter."));
        
        mockMvc.perform(get("/api/internal/gmail/oauth2/callback")
                .param("code", "valid-code")
                .param("state", "invalid-state"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired OAuth state parameter."));
    }
}

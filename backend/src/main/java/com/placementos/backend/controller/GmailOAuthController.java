package com.placementos.backend.controller;

import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.service.GmailOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/internal/gmail/oauth2")
public class GmailOAuthController {

    private final GmailOAuthService gmailOAuthService;

    public GmailOAuthController(GmailOAuthService gmailOAuthService) {
        this.gmailOAuthService = gmailOAuthService;
    }

    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) throws IOException {
        String authUrl = gmailOAuthService.generateAuthorizationUrl();
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("OAuth Authorization Error: " + error);
        }

        if (code == null || state == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing code or state parameter.");
        }

        try {
            GmailSource source = gmailOAuthService.handleCallback(code, state);
            return ResponseEntity.ok("Successfully authenticated and registered Gmail Source: " + source.getEmailAddress());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during authentication: " + e.getMessage());
        }
    }
}

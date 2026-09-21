package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.auth.AuthResponse;
import com.placementos.backend.domain.dto.auth.LoginRequest;
import com.placementos.backend.domain.dto.auth.RegisterRequest;
import com.placementos.backend.domain.dto.auth.UserAccountResponse;
import com.placementos.backend.domain.service.AccountDeletionService;
import com.placementos.backend.domain.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST API for user registration, authentication, and session identity.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    public AuthController(AuthService authService,
                          AccountDeletionService accountDeletionService) {
        this.authService = authService;
        this.accountDeletionService = accountDeletionService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserAccountResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserAccountResponse user = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * Authenticated account deletion.
     * Identity is derived from the authenticated principal — never from a client-supplied parameter.
     * Idempotent: safe to call multiple times.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        accountDeletionService.deleteAccount(principal.getName());
        return ResponseEntity.ok(Map.of("status", "DELETED", "message", "Account successfully deleted."));
    }
}

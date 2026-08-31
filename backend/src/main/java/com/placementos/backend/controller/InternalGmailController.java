package com.placementos.backend.controller;

import com.placementos.backend.domain.service.GmailHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal/Development endpoints for Gmail ingestion lifecycle.
 * NOT intended for student or public API access.
 */
@RestController
@RequestMapping("/api/internal/gmail")
public class InternalGmailController {

    private final GmailHistoryService gmailHistoryService;

    public InternalGmailController(GmailHistoryService gmailHistoryService) {
        this.gmailHistoryService = gmailHistoryService;
    }

    /**
     * Manually triggers a Gmail History API synchronization for the given source.
     * Useful for manual end-to-end testing without waiting for a Google Pub/Sub push.
     */
    @PostMapping("/history/sync")
    public ResponseEntity<Void> triggerHistorySync(@RequestParam("email") String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        gmailHistoryService.syncHistory(emailAddress);
        
        return ResponseEntity.ok().build();
    }
}

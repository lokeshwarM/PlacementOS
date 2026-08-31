package com.placementos.backend.controller;

import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.service.GmailHistoryService;
import com.placementos.backend.domain.service.GmailMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal/Development endpoints for Gmail ingestion lifecycle.
 * NOT intended for student or public API access.
 */
@RestController
@RequestMapping("/api/internal/gmail")
public class InternalGmailController {

    private final GmailHistoryService gmailHistoryService;
    private final GmailMessageService gmailMessageService;

    public InternalGmailController(GmailHistoryService gmailHistoryService,
                                   GmailMessageService gmailMessageService) {
        this.gmailHistoryService = gmailHistoryService;
        this.gmailMessageService = gmailMessageService;
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

    /**
     * Manually triggers Gmail message retrieval and normalization for a specific messageId and source.
     * Useful for manual testing of MIME parsing and attachment extraction.
     */
    @PostMapping("/messages/retrieve")
    public ResponseEntity<Map<String, Object>> retrieveMessage(@RequestParam("email") String emailAddress,
                                                               @RequestParam("messageId") String messageId) {
        if (emailAddress == null || emailAddress.isBlank() || messageId == null || messageId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        GmailMessage message = gmailMessageService.retrieveAndPersistMessage(emailAddress, messageId);
        if (message == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "id", message.getId() != null ? message.getId() : 0L,
                "messageId", message.getMessageId() != null ? message.getMessageId() : "",
                "status", message.getRetrievalStatus() != null ? message.getRetrievalStatus().name() : "UNKNOWN",
                "attachmentCount", message.getAttachments() != null ? message.getAttachments().size() : 0
        ));
    }
}

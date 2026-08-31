package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.StructuredPlacementExtractionResult;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.repository.GmailMessageRepository;
import com.placementos.backend.domain.service.PlacementIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for internal communication between Python processing service and Spring Boot.
 * Python submits validated extraction results here; Spring Boot validates and persists business state.
 */
@RestController
@RequestMapping
public class InternalExtractionController {

    private static final Logger log = LoggerFactory.getLogger(InternalExtractionController.class);

    private final PlacementIngestionService placementIngestionService;
    private final GmailMessageRepository gmailMessageRepository;

    public InternalExtractionController(PlacementIngestionService placementIngestionService,
                                        GmailMessageRepository gmailMessageRepository) {
        this.placementIngestionService = placementIngestionService;
        this.gmailMessageRepository = gmailMessageRepository;
    }

    /**
     * Callback endpoint where Python processing worker posts the structured extraction result.
     */
    @PostMapping("/api/v1/internal/extraction/result")
    public ResponseEntity<Map<String, Object>> receiveExtractionResult(
            @RequestBody StructuredPlacementExtractionResult result) {
        log.info("Received extraction result from processing service for messageId={}", result.getMessageId());

        Optional<PlacementDrive> driveOpt = placementIngestionService.ingestExtractionResult(result);

        if (driveOpt.isPresent()) {
            PlacementDrive drive = driveOpt.get();
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "messageId", result.getMessageId(),
                    "driveId", drive.getId(),
                    "companyName", drive.getCompanyName(),
                    "roleCount", drive.getRoles().size()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "status", "RECORDED",
                    "messageId", result.getMessageId() != null ? result.getMessageId() : "",
                    "isPlacement", result.getClassification() != null ? Boolean.TRUE.equals(result.getClassification().getIsPlacement()) : false
            ));
        }
    }

    /**
     * Endpoint to fetch stored message text and metadata by message ID.
     */
    @GetMapping("/api/v1/internal/messages/{messageId}")
    public ResponseEntity<Map<String, Object>> getMessageForProcessing(@PathVariable String messageId) {
        return gmailMessageRepository.findByMessageId(messageId)
                .map(msg -> ResponseEntity.ok(Map.<String, Object>of(
                        "messageId", msg.getMessageId(),
                        "sourceEmail", msg.getGmailSource().getEmailAddress(),
                        "subject", msg.getSubject() != null ? msg.getSubject() : "",
                        "sender", msg.getSender() != null ? msg.getSender() : "",
                        "recipients", msg.getRecipients() != null ? msg.getRecipients() : "",
                        "plainTextBody", msg.getPlainTextBody() != null ? msg.getPlainTextBody() : "",
                        "snippet", msg.getSnippet() != null ? msg.getSnippet() : "",
                        "internalDate", msg.getGmailInternalDate() != null ? msg.getGmailInternalDate().toString() : ""
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

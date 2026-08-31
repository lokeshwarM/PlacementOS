package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.DocumentProcessingResultDto;
import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.ShortlistEntry;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.ShortlistEntryRepository;
import com.placementos.backend.domain.service.GmailAttachmentService;
import com.placementos.backend.domain.service.ShortlistMatchingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller exposing protected internal endpoints for attachment content retrieval,
 * document processing callbacks, and shortlist inspection.
 */
@RestController
@RequestMapping("/api/v1/internal")
public class InternalShortlistController {

    private final GmailAttachmentService gmailAttachmentService;
    private final ShortlistMatchingService shortlistMatchingService;
    private final ShortlistEntryRepository shortlistEntryRepository;
    private final String internalServiceKey;

    public InternalShortlistController(GmailAttachmentService gmailAttachmentService,
                                       ShortlistMatchingService shortlistMatchingService,
                                       ShortlistEntryRepository shortlistEntryRepository,
                                       @Value("${app.internal.service-key:dev-internal-key}") String internalServiceKey) {
        this.gmailAttachmentService = gmailAttachmentService;
        this.shortlistMatchingService = shortlistMatchingService;
        this.shortlistEntryRepository = shortlistEntryRepository;
        this.internalServiceKey = internalServiceKey;
    }

    /**
     * Internal endpoint to securely stream attachment content to the document processing worker.
     */
    @GetMapping("/attachments/{id}/content")
    public ResponseEntity<byte[]> getAttachmentContent(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        validateInternalKey(serviceKey);

        try {
            byte[] content = gmailAttachmentService.getAttachmentContent(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attachment-" + id + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Attachment content not available for id: " + id);
        }
    }

    /**
     * Callback for Python document worker to submit structured shortlist extraction results.
     */
    @PostMapping("/shortlists/result")
    public ResponseEntity<List<ShortlistEntry>> submitShortlistResult(
            @RequestBody DocumentProcessingResultDto resultDto,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        validateInternalKey(serviceKey);

        List<ShortlistEntry> entries = shortlistMatchingService.processDocumentShortlist(resultDto);
        return ResponseEntity.ok(entries);
    }

    /**
     * Manual development trigger to download and persist an attachment from Gmail.
     */
    @PostMapping("/attachments/{id}/process")
    public ResponseEntity<Attachment> triggerAttachmentDownload(@PathVariable Long id) {
        Attachment attachment = gmailAttachmentService.downloadAndStoreAttachment(id);
        return ResponseEntity.ok(attachment);
    }

    /**
     * Inspection endpoint for ambiguous and review-required shortlist entries.
     */
    @GetMapping("/shortlists/unresolved")
    public ResponseEntity<List<ShortlistEntry>> getUnresolvedEntries() {
        List<ShortlistEntry> ambiguous = shortlistEntryRepository.findByMatchStatus(ShortlistMatchStatus.AMBIGUOUS);
        List<ShortlistEntry> reviewReq = shortlistEntryRepository.findByMatchStatus(ShortlistMatchStatus.REVIEW_REQUIRED);
        ambiguous.addAll(reviewReq);
        return ResponseEntity.ok(ambiguous);
    }

    private void validateInternalKey(String serviceKey) {
        if (serviceKey == null || !serviceKey.equals(internalServiceKey)) {
            throw new SecurityException("Unauthorized internal service request");
        }
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}

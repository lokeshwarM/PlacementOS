package com.placementos.backend.domain.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.placementos.backend.domain.dto.NormalizedAttachment;
import com.placementos.backend.domain.dto.NormalizedGmailMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Normalizes Google Gmail API MIME Message structures into stable internal DTOs.
 * Handles nested multipart traversal, Base64URL body decoding, case-insensitive
 * header extraction, and attachment metadata isolation.
 */
@Component
public class GmailMimeNormalizer {

    public NormalizedGmailMessage normalize(Message gmailMessage) {
        if (gmailMessage == null) {
            return null;
        }

        NormalizedGmailMessage normalized = new NormalizedGmailMessage();
        normalized.setMessageId(gmailMessage.getId());
        normalized.setThreadId(gmailMessage.getThreadId());
        normalized.setSnippet(gmailMessage.getSnippet());

        if (gmailMessage.getInternalDate() != null) {
            normalized.setInternalDate(Instant.ofEpochMilli(gmailMessage.getInternalDate()));
        }

        MessagePart rootPayload = gmailMessage.getPayload();
        if (rootPayload != null) {
            // Extract headers
            extractHeaders(rootPayload.getHeaders(), normalized);

            // Traverse MIME parts
            StringBuilder plainTextBuffer = new StringBuilder();
            StringBuilder htmlBuffer = new StringBuilder();
            List<NormalizedAttachment> attachments = new ArrayList<>();

            traverseMimePart(rootPayload, plainTextBuffer, htmlBuffer, attachments);

            normalized.setPlainTextBody(plainTextBuffer.length() > 0 ? plainTextBuffer.toString() : null);
            normalized.setHtmlBody(htmlBuffer.length() > 0 ? htmlBuffer.toString() : null);
            normalized.setAttachments(attachments);
        }

        return normalized;
    }

    private void extractHeaders(List<MessagePartHeader> headers, NormalizedGmailMessage normalized) {
        if (headers == null || headers.isEmpty()) {
            return;
        }

        String to = null;
        String cc = null;

        for (MessagePartHeader header : headers) {
            String name = header.getName();
            String value = header.getValue();

            if (name == null || value == null) {
                continue;
            }

            if (name.equalsIgnoreCase("Subject")) {
                normalized.setSubject(value);
            } else if (name.equalsIgnoreCase("From")) {
                normalized.setSender(value);
            } else if (name.equalsIgnoreCase("To")) {
                to = value;
            } else if (name.equalsIgnoreCase("Cc")) {
                cc = value;
            }
        }

        if (to != null && cc != null) {
            normalized.setRecipients(to + ", " + cc);
        } else if (to != null) {
            normalized.setRecipients(to);
        } else if (cc != null) {
            normalized.setRecipients(cc);
        }
    }

    private void traverseMimePart(MessagePart part,
                                  StringBuilder plainTextBuffer,
                                  StringBuilder htmlBuffer,
                                  List<NormalizedAttachment> attachments) {
        if (part == null) {
            return;
        }

        String filename = part.getFilename();
        String mimeType = part.getMimeType();

        // Check if this part is an attachment
        boolean isAttachment = filename != null && !filename.trim().isEmpty()
                && part.getBody() != null
                && (part.getBody().getAttachmentId() != null || (part.getBody().getSize() != null && part.getBody().getSize() > 0));

        if (isAttachment) {
            String attachmentId = part.getBody().getAttachmentId();
            Long size = part.getBody().getSize() != null ? part.getBody().getSize().longValue() : null;
            attachments.add(new NormalizedAttachment(filename, mimeType, attachmentId, size));
            return;
        }

        // Check body content
        if (part.getBody() != null && part.getBody().getData() != null) {
            String decoded = decodeBase64Url(part.getBody().getData());
            if (decoded != null && !decoded.isEmpty()) {
                if (mimeType != null && mimeType.equalsIgnoreCase("text/plain")) {
                    if (plainTextBuffer.length() > 0) {
                        plainTextBuffer.append("\n");
                    }
                    plainTextBuffer.append(decoded);
                } else if (mimeType != null && mimeType.equalsIgnoreCase("text/html")) {
                    if (htmlBuffer.length() > 0) {
                        htmlBuffer.append("\n");
                    }
                    htmlBuffer.append(decoded);
                }
            }
        }

        // Recursively traverse nested parts (e.g. multipart/mixed, multipart/alternative)
        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                traverseMimePart(childPart, plainTextBuffer, htmlBuffer, attachments);
            }
        }
    }

    private String decodeBase64Url(String base64UrlData) {
        if (base64UrlData == null || base64UrlData.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64UrlData);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Defensive: fallback to standard decoder if URL decoding fails
            try {
                byte[] bytes = Base64.getDecoder().decode(base64UrlData);
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}

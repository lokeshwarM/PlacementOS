package com.placementos.backend.domain.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.placementos.backend.domain.dto.NormalizedGmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GmailMimeNormalizerTest {

    private GmailMimeNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new GmailMimeNormalizer();
    }

    private String encodeBase64Url(String content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void normalize_nullMessage_returnsNull() {
        assertNull(normalizer.normalize(null));
    }

    @Test
    void normalize_simplePlainText() {
        Message message = new Message()
                .setId("msg-1")
                .setThreadId("th-1")
                .setSnippet("Job Opportunity")
                .setInternalDate(1700000000000L);

        MessagePart root = new MessagePart()
                .setMimeType("text/plain")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Campus Placement Notice"),
                        new MessagePartHeader().setName("from").setValue("cdc@vit.ac.in"),
                        new MessagePartHeader().setName("TO").setValue("student@vit.ac.in")
                ))
                .setBody(new MessagePartBody().setData(encodeBase64Url("Hello Students, Microsoft drive is open.")));

        message.setPayload(root);

        NormalizedGmailMessage result = normalizer.normalize(message);

        assertNotNull(result);
        assertEquals("msg-1", result.getMessageId());
        assertEquals("th-1", result.getThreadId());
        assertEquals("Campus Placement Notice", result.getSubject());
        assertEquals("cdc@vit.ac.in", result.getSender());
        assertEquals("student@vit.ac.in", result.getRecipients());
        assertEquals("Hello Students, Microsoft drive is open.", result.getPlainTextBody());
        assertNull(result.getHtmlBody());
        assertTrue(result.getAttachments().isEmpty());
    }

    @Test
    void normalize_simpleHtml() {
        Message message = new Message().setId("msg-2");

        MessagePart root = new MessagePart()
                .setMimeType("text/html")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("subject").setValue("Google Drive"),
                        new MessagePartHeader().setName("FROM").setValue("cdc@vit.ac.in"),
                        new MessagePartHeader().setName("To").setValue("student1@vit.ac.in"),
                        new MessagePartHeader().setName("CC").setValue("hod@vit.ac.in")
                ))
                .setBody(new MessagePartBody().setData(encodeBase64Url("<p>Google drive details</p>")));

        message.setPayload(root);

        NormalizedGmailMessage result = normalizer.normalize(message);

        assertNotNull(result);
        assertEquals("Google Drive", result.getSubject());
        assertEquals("cdc@vit.ac.in", result.getSender());
        assertEquals("student1@vit.ac.in, hod@vit.ac.in", result.getRecipients());
        assertNull(result.getPlainTextBody());
        assertEquals("<p>Google drive details</p>", result.getHtmlBody());
    }

    @Test
    void normalize_multipartAlternative_extractsBothPlainAndHtml() {
        Message message = new Message().setId("msg-3");

        MessagePart textPart = new MessagePart()
                .setMimeType("text/plain")
                .setBody(new MessagePartBody().setData(encodeBase64Url("Plain Text Content")));

        MessagePart htmlPart = new MessagePart()
                .setMimeType("text/html")
                .setBody(new MessagePartBody().setData(encodeBase64Url("<b>HTML Content</b>")));

        MessagePart root = new MessagePart()
                .setMimeType("multipart/alternative")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Drive Announcement")
                ))
                .setParts(List.of(textPart, htmlPart));

        message.setPayload(root);

        NormalizedGmailMessage result = normalizer.normalize(message);

        assertNotNull(result);
        assertEquals("Plain Text Content", result.getPlainTextBody());
        assertEquals("<b>HTML Content</b>", result.getHtmlBody());
        assertTrue(result.getAttachments().isEmpty());
    }

    @Test
    void normalize_nestedMultipartWithAttachments() {
        Message message = new Message().setId("msg-4");

        // Nested multipart/alternative
        MessagePart textPart = new MessagePart()
                .setMimeType("text/plain")
                .setBody(new MessagePartBody().setData(encodeBase64Url("Please find attached JD.")));

        MessagePart htmlPart = new MessagePart()
                .setMimeType("text/html")
                .setBody(new MessagePartBody().setData(encodeBase64Url("<p>Please find attached JD.</p>")));

        MessagePart alternativePart = new MessagePart()
                .setMimeType("multipart/alternative")
                .setParts(List.of(textPart, htmlPart));

        // Attachments
        MessagePart att1 = new MessagePart()
                .setMimeType("application/pdf")
                .setFilename("Job_Description.pdf")
                .setBody(new MessagePartBody().setAttachmentId("att-id-101").setSize(102400));

        MessagePart att2 = new MessagePart()
                .setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .setFilename("Shortlist.xlsx")
                .setBody(new MessagePartBody().setAttachmentId("att-id-102").setSize(20480));

        MessagePart root = new MessagePart()
                .setMimeType("multipart/mixed")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Amazon JD & Shortlist")
                ))
                .setParts(List.of(alternativePart, att1, att2));

        message.setPayload(root);

        NormalizedGmailMessage result = normalizer.normalize(message);

        assertNotNull(result);
        assertEquals("Please find attached JD.", result.getPlainTextBody());
        assertEquals("<p>Please find attached JD.</p>", result.getHtmlBody());
        assertEquals(2, result.getAttachments().size());

        assertEquals("Job_Description.pdf", result.getAttachments().get(0).getFilename());
        assertEquals("application/pdf", result.getAttachments().get(0).getContentType());
        assertEquals("att-id-101", result.getAttachments().get(0).getAttachmentId());
        assertEquals(102400L, result.getAttachments().get(0).getByteSize());

        assertEquals("Shortlist.xlsx", result.getAttachments().get(1).getFilename());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.getAttachments().get(1).getContentType());
        assertEquals("att-id-102", result.getAttachments().get(1).getAttachmentId());
        assertEquals(20480L, result.getAttachments().get(1).getByteSize());
    }

    @Test
    void normalize_missingBody_handlesGracefully() {
        Message message = new Message().setId("msg-5");
        MessagePart root = new MessagePart()
                .setMimeType("text/plain")
                .setHeaders(List.of(new MessagePartHeader().setName("Subject").setValue("Empty Email")))
                .setBody(new MessagePartBody()); // null data

        message.setPayload(root);

        NormalizedGmailMessage result = normalizer.normalize(message);

        assertNotNull(result);
        assertEquals("Empty Email", result.getSubject());
        assertNull(result.getPlainTextBody());
        assertNull(result.getHtmlBody());
    }
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.enums.GmailMessageRetrievalStatus;
import com.placementos.backend.domain.model.GmailSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GmailMessageRepositoryTest {

    @Autowired
    private GmailMessageRepository gmailMessageRepository;

    @Autowired
    private GmailSourceRepository gmailSourceRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    void saveAndFind_persistsMessageWithAttachmentsAndRetrievesCorrectly() {
        // Create source
        GmailSource source = new GmailSource();
        source.setEmailAddress("cdc-test-" + System.currentTimeMillis() + "@vit.ac.in");
        source.setProvider("GOOGLE");
        source.setStatus("ACTIVE");
        source = gmailSourceRepository.saveAndFlush(source);

        // Create message
        GmailMessage message = new GmailMessage();
        message.setMessageId("msg-repo-test-1");
        message.setGmailSource(source);
        message.setThreadId("th-repo-test-1");
        message.setSubject("Microsoft FTE Hiring 2026");
        message.setSender("cdc@vit.ac.in");
        message.setRecipients("students@vit.ac.in");
        message.setPlainTextBody("Please find JD attached.");
        message.setHtmlBody("<p>Please find JD attached.</p>");
        message.setSnippet("Please find JD attached.");
        message.setGmailInternalDate(Instant.now());
        message.setRetrievalStatus(GmailMessageRetrievalStatus.RETRIEVED);
        message.setRetrievedAt(Instant.now());

        // Attachments
        Attachment att1 = new Attachment();
        att1.setFilename("Microsoft_JD.pdf");
        att1.setContentType("application/pdf");
        att1.setAttachmentId("att-gmail-id-1");
        att1.setByteSize(102400L);
        message.addAttachment(att1);

        GmailMessage saved = gmailMessageRepository.saveAndFlush(message);

        assertNotNull(saved.getId());

        // Find by source ID and message ID
        Optional<GmailMessage> found = gmailMessageRepository.findByGmailSourceIdAndMessageId(source.getId(), "msg-repo-test-1");
        assertTrue(found.isPresent());
        assertEquals("Microsoft FTE Hiring 2026", found.get().getSubject());
        assertEquals(1, found.get().getAttachments().size());

        // Find attachment by gmailMessageId
        List<Attachment> attachments = attachmentRepository.findByGmailMessageId(saved.getId());
        assertEquals(1, attachments.size());
        assertEquals("Microsoft_JD.pdf", attachments.get(0).getFilename());
        assertEquals("att-gmail-id-1", attachments.get(0).getAttachmentId());
    }
}

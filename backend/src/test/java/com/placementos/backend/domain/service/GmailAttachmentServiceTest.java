package com.placementos.backend.domain.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.MessagePartBody;
import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import com.placementos.backend.domain.repository.AttachmentRepository;
import com.placementos.backend.domain.service.storage.AttachmentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailAttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AttachmentStorage attachmentStorage;

    @Mock
    private GmailOAuthService gmailOAuthService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Gmail gmailClient;

    private GmailAttachmentService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        service = new GmailAttachmentService(
                attachmentRepository,
                attachmentStorage,
                gmailOAuthService,
                redisTemplate
        );
    }

    @Test
    void downloadAndStoreAttachment_success() throws IOException {
        GmailSource source = new GmailSource();
        source.setEmailAddress("cdc@example.com");
        source.setProvider("GOOGLE");
        GmailMessage message = new GmailMessage();
        message.setId(10L);
        message.setMessageId("msg-gmail-att-1");
        message.setGmailSource(source);

        Attachment attachment = new Attachment();
        attachment.setFilename("Shortlist.xlsx");
        attachment.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        attachment.setAttachmentId("att-gmail-xyz");
        attachment.setGmailMessage(message);

        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        when(gmailOAuthService.getGmailClientForSource("cdc@example.com")).thenReturn(gmailClient);

        byte[] rawBytes = "Excel Binary Mock Data".getBytes();
        String base64Data = Base64.getUrlEncoder().encodeToString(rawBytes);

        MessagePartBody partBody = new MessagePartBody().setData(base64Data);
        when(gmailClient.users().messages().attachments().get("me", "msg-gmail-att-1", "att-gmail-xyz").execute())
                .thenReturn(partBody);

        when(attachmentStorage.store(anyString(), any(byte[].class), anyString()))
                .thenReturn("attachments/10/uuid_Shortlist.xlsx");
        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Attachment result = service.downloadAndStoreAttachment(100L);

        assertNotNull(result);
        assertEquals("attachments/10/uuid_Shortlist.xlsx", result.getStorageReference());
        assertEquals((long) rawBytes.length, result.getByteSize());
        assertNotNull(result.getSha256Checksum());
        assertEquals(AttachmentParsedStatus.DOWNLOADED, result.getParsedStatus());

        verify(attachmentStorage).store(anyString(), eq(rawBytes), anyString());
        verify(streamOperations).add(any());
    }

    @Test
    void downloadAndStoreAttachment_alreadyDownloaded_skips() {
        Attachment attachment = new Attachment();
        attachment.setStorageReference("attachments/10/existing.xlsx");

        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        when(attachmentStorage.exists("attachments/10/existing.xlsx")).thenReturn(true);

        Attachment result = service.downloadAndStoreAttachment(100L);

        assertSame(attachment, result);
        verifyNoInteractions(gmailOAuthService);
    }
}

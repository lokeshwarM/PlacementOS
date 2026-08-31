package com.placementos.backend.domain.service;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.placementos.backend.domain.dto.NormalizedAttachment;
import com.placementos.backend.domain.dto.NormalizedGmailMessage;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.enums.GmailMessageRetrievalStatus;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailMessageRepository;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailMessageServiceTest {

    @Mock
    private GmailSourceRepository sourceRepository;

    @Mock
    private GmailOAuthService oauthService;

    @Mock
    private GmailMimeNormalizer mimeNormalizer;

    @Mock
    private GmailMessageRepository gmailMessageRepository;

    @Mock
    private ProcessedEmailService processedEmailService;

    @Mock
    private Gmail gmailClient;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Messages messages;

    @Mock
    private Gmail.Users.Messages.Get messagesGetRequest;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.StreamOperations<String, Object, Object> streamOps;

    private tools.jackson.databind.ObjectMapper objectMapper;
    private GmailMessageService messageService;

    private static final String SOURCE_EMAIL = "cdc@example.com";
    private static final String MESSAGE_ID = "msg-12345";

    @BeforeEach
    void setUp() {
        objectMapper = new tools.jackson.databind.ObjectMapper();
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);
        messageService = new GmailMessageService(
                sourceRepository,
                oauthService,
                mimeNormalizer,
                gmailMessageRepository,
                processedEmailService,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    void retrieveAndPersistMessage_sourceNotFound_marksFailedAndReturnsNull() {
        when(sourceRepository.findByEmailAddress(SOURCE_EMAIL)).thenReturn(Optional.empty());

        GmailMessage result = messageService.retrieveAndPersistMessage(SOURCE_EMAIL, MESSAGE_ID);

        assertNull(result);
        verify(processedEmailService).markFailed(eq(MESSAGE_ID), contains("Gmail source not found"));
        verifyNoInteractions(oauthService);
    }

    @Test
    void retrieveAndPersistMessage_alreadyRetrieved_returnsExistingIdempotently() {
        GmailSource source = new GmailSource();
        source.setId(1L);
        source.setEmailAddress(SOURCE_EMAIL);
        when(sourceRepository.findByEmailAddress(SOURCE_EMAIL)).thenReturn(Optional.of(source));

        GmailMessage existing = new GmailMessage();
        existing.setId(10L);
        existing.setMessageId(MESSAGE_ID);
        existing.setRetrievalStatus(GmailMessageRetrievalStatus.RETRIEVED);

        when(gmailMessageRepository.findByGmailSourceIdAndMessageId(1L, MESSAGE_ID)).thenReturn(Optional.of(existing));

        GmailMessage result = messageService.retrieveAndPersistMessage(SOURCE_EMAIL, MESSAGE_ID);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(processedEmailService).markRetrieved(MESSAGE_ID);
        verifyNoInteractions(oauthService);
    }

    @Test
    void retrieveAndPersistMessage_successfulRetrievalAndPersistence() throws Exception {
        GmailSource source = new GmailSource();
        source.setId(1L);
        source.setEmailAddress(SOURCE_EMAIL);
        when(sourceRepository.findByEmailAddress(SOURCE_EMAIL)).thenReturn(Optional.of(source));
        when(gmailMessageRepository.findByGmailSourceIdAndMessageId(1L, MESSAGE_ID)).thenReturn(Optional.empty());

        when(oauthService.getGmailClientForSource(SOURCE_EMAIL)).thenReturn(gmailClient);
        when(gmailClient.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.get("me", MESSAGE_ID)).thenReturn(messagesGetRequest);
        when(messagesGetRequest.setFormat("full")).thenReturn(messagesGetRequest);

        Message rawMsg = new Message().setId(MESSAGE_ID).setThreadId("th-99");
        when(messagesGetRequest.execute()).thenReturn(rawMsg);

        NormalizedGmailMessage normalized = new NormalizedGmailMessage();
        normalized.setMessageId(MESSAGE_ID);
        normalized.setThreadId("th-99");
        normalized.setSubject("Placement Drive 2026");
        normalized.setSender("cdc@vit.ac.in");
        normalized.setRecipients("students@vit.ac.in");
        normalized.setPlainTextBody("Please apply before deadline.");
        normalized.setHtmlBody("<p>Please apply before deadline.</p>");
        normalized.setSnippet("Please apply...");
        normalized.setInternalDate(Instant.now());
        normalized.setAttachments(List.of(
                new NormalizedAttachment("JD.pdf", "application/pdf", "att-001", 50000L)
        ));

        when(mimeNormalizer.normalize(rawMsg)).thenReturn(normalized);
        when(gmailMessageRepository.saveAndFlush(any(GmailMessage.class))).thenAnswer(inv -> {
            GmailMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        GmailMessage result = messageService.retrieveAndPersistMessage(SOURCE_EMAIL, MESSAGE_ID);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertEquals("Placement Drive 2026", result.getSubject());
        assertEquals(1, result.getAttachments().size());
        assertEquals("JD.pdf", result.getAttachments().get(0).getFilename());

        verify(processedEmailService).markRetrieved(MESSAGE_ID);
    }

    @Test
    void retrieveAndPersistMessage_api404_marksFailedAndReturnsNull() throws Exception {
        GmailSource source = new GmailSource();
        source.setId(1L);
        source.setEmailAddress(SOURCE_EMAIL);
        when(sourceRepository.findByEmailAddress(SOURCE_EMAIL)).thenReturn(Optional.of(source));
        when(gmailMessageRepository.findByGmailSourceIdAndMessageId(1L, MESSAGE_ID)).thenReturn(Optional.empty());

        when(oauthService.getGmailClientForSource(SOURCE_EMAIL)).thenReturn(gmailClient);
        when(gmailClient.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.get("me", MESSAGE_ID)).thenReturn(messagesGetRequest);
        when(messagesGetRequest.setFormat("full")).thenReturn(messagesGetRequest);

        HttpResponseException.Builder builder = new HttpResponseException.Builder(404, "Not Found", new HttpHeaders());
        GoogleJsonError jsonError = new GoogleJsonError();
        jsonError.setCode(404);
        jsonError.setMessage("Requested entity was not found.");
        GoogleJsonResponseException notFoundException = new GoogleJsonResponseException(builder, jsonError);

        when(messagesGetRequest.execute()).thenThrow(notFoundException);

        GmailMessage result = messageService.retrieveAndPersistMessage(SOURCE_EMAIL, MESSAGE_ID);

        assertNull(result);
        verify(processedEmailService).markFailed(eq(MESSAGE_ID), contains("404"));
        verify(gmailMessageRepository, never()).saveAndFlush(any());
    }

    @Test
    void retrieveAndPersistMessage_transientError_throwsExceptionForRetry() throws Exception {
        GmailSource source = new GmailSource();
        source.setId(1L);
        source.setEmailAddress(SOURCE_EMAIL);
        when(sourceRepository.findByEmailAddress(SOURCE_EMAIL)).thenReturn(Optional.of(source));
        when(gmailMessageRepository.findByGmailSourceIdAndMessageId(1L, MESSAGE_ID)).thenReturn(Optional.empty());

        when(oauthService.getGmailClientForSource(SOURCE_EMAIL)).thenReturn(gmailClient);
        when(gmailClient.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.get("me", MESSAGE_ID)).thenReturn(messagesGetRequest);
        when(messagesGetRequest.setFormat("full")).thenReturn(messagesGetRequest);

        when(messagesGetRequest.execute()).thenThrow(new IOException("Connection timed out"));

        assertThrows(RuntimeException.class, () -> messageService.retrieveAndPersistMessage(SOURCE_EMAIL, MESSAGE_ID));
    }
}

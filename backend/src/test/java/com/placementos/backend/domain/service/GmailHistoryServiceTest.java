package com.placementos.backend.domain.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import com.placementos.backend.domain.event.EventEnvelope;
import com.placementos.backend.domain.event.EventType;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailHistoryServiceTest {

    @Mock
    private GmailSourceRepository sourceRepository;

    @Mock
    private GmailOAuthService oauthService;

    @Mock
    private ProcessedEmailService processedEmailService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Gmail gmailClient;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.History historyEndpoint;

    @Mock
    private Gmail.Users.History.List historyListRequest;

    private GmailHistoryService gmailHistoryService;

    private static final String EMAIL = "cdc@example.com";

    @BeforeEach
    void setUp() {
        gmailHistoryService = new GmailHistoryService(
                sourceRepository, oauthService, processedEmailService, redisTemplate);
    }

    @Test
    void syncHistory_noSource_doesNothing() {
        when(sourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.empty());

        gmailHistoryService.syncHistory(EMAIL);

        verifyNoInteractions(oauthService);
    }

    @Test
    void syncHistory_noStartHistoryId_doesNothing() {
        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        source.setLastHistoryId(null);
        when(sourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        gmailHistoryService.syncHistory(EMAIL);

        verifyNoInteractions(oauthService);
    }

    @Test
    void syncHistory_success_paginatesAndPublishesAndAdvancesCursor() throws Exception {
        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        source.setLastHistoryId("1000");
        when(sourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        when(oauthService.getGmailClientForSource(EMAIL)).thenReturn(gmailClient);
        when(gmailClient.users()).thenReturn(users);
        when(users.history()).thenReturn(historyEndpoint);
        when(historyEndpoint.list("me")).thenReturn(historyListRequest);
        when(historyListRequest.setStartHistoryId(new BigInteger("1000"))).thenReturn(historyListRequest);
        when(historyListRequest.setHistoryTypes(List.of("messageAdded"))).thenReturn(historyListRequest);
        when(historyListRequest.setPageToken(any())).thenReturn(historyListRequest); // For page 2

        // Page 1
        Message msg1 = new Message().setId("msg1").setThreadId("thread1");
        HistoryMessageAdded hma1 = new HistoryMessageAdded().setMessage(msg1);
        History h1 = new History().setMessagesAdded(List.of(hma1));

        ListHistoryResponse response1 = new ListHistoryResponse()
                .setHistoryId(new BigInteger("1010"))
                .setHistory(List.of(h1))
                .setNextPageToken("page2");

        // Page 2
        Message msg2 = new Message().setId("msg2").setThreadId("thread2");
        HistoryMessageAdded hma2 = new HistoryMessageAdded().setMessage(msg2);
        History h2 = new History().setMessagesAdded(List.of(hma2));

        ListHistoryResponse response2 = new ListHistoryResponse()
                .setHistoryId(new BigInteger("1020"))
                .setHistory(List.of(h2))
                .setNextPageToken(null);

        when(historyListRequest.execute())
                .thenReturn(response1)
                .thenReturn(response2);

        // Idempotency responses: msg1 succeeds, msg2 is a duplicate (returns false)
        when(processedEmailService.tryAcquireDiscovery(eq("msg1"), eq("thread1"), eq(EMAIL), any(Instant.class)))
                .thenReturn(true);
        when(processedEmailService.tryAcquireDiscovery(eq("msg2"), eq("thread2"), eq(EMAIL), any(Instant.class)))
                .thenReturn(false);

        // Execute
        gmailHistoryService.syncHistory(EMAIL);

        // Verify publication only for msg1
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(redisTemplate, times(1)).convertAndSend(eq("placementos.events"), eventCaptor.capture());
        assertEquals(EventType.GMAIL_MESSAGE_DISCOVERED, eventCaptor.getValue().getEventType());

        // Verify markQueued only for msg1
        verify(processedEmailService).markQueued("msg1");
        verify(processedEmailService, never()).markQueued("msg2");

        // Verify cursor advanced to 1020
        assertEquals("1020", source.getLastHistoryId());
        verify(sourceRepository).save(source);
    }
}

package com.placementos.backend.domain.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GmailWatchServiceTest {

    @Mock
    private GmailOAuthService gmailOAuthService;

    @Mock
    private GmailSourceRepository gmailSourceRepository;

    @Mock
    private Gmail gmail;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Watch watchMethod;

    @InjectMocks
    private GmailWatchService gmailWatchService;

    private static final String EMAIL = "cdc@example.com";
    private static final String TOPIC = "projects/test-project/topics/gmail-notifications";

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(gmailWatchService, "pubSubTopic", TOPIC);

        when(gmailOAuthService.getGmailClientForSource(EMAIL)).thenReturn(gmail);
        when(gmail.users()).thenReturn(users);
        when(users.watch(eq("me"), any(WatchRequest.class))).thenReturn(watchMethod);
    }

    @Test
    public void registerWatch_sendsCorrectTopicInRequest() throws IOException {
        WatchResponse response = new WatchResponse()
                .setHistoryId(new BigInteger("12345678"))
                .setExpiration(System.currentTimeMillis() + 604800000L);
        when(watchMethod.execute()).thenReturn(response);

        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        gmailWatchService.registerWatch(EMAIL);

        ArgumentCaptor<WatchRequest> captor = ArgumentCaptor.forClass(WatchRequest.class);
        verify(users).watch(eq("me"), captor.capture());
        assertEquals(TOPIC, captor.getValue().getTopicName());
        assertTrue(captor.getValue().getLabelIds().contains("INBOX"));
    }

    @Test
    public void registerWatch_persistsHistoryIdAsString() throws IOException {
        String expectedHistoryId = "98765432";
        WatchResponse response = new WatchResponse()
                .setHistoryId(new BigInteger(expectedHistoryId))
                .setExpiration(System.currentTimeMillis() + 604800000L);
        when(watchMethod.execute()).thenReturn(response);

        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailSource result = gmailWatchService.registerWatch(EMAIL);

        // historyId must be stored as the opaque String from the API — not parsed as a number
        assertEquals(expectedHistoryId, result.getLastHistoryId());
        assertEquals("ACTIVE", result.getWatchStatus());
    }

    @Test
    public void registerWatch_persistsWatchExpiration() throws IOException {
        long expirationEpochMs = System.currentTimeMillis() + 604800000L;
        WatchResponse response = new WatchResponse()
                .setHistoryId(new BigInteger("11111111"))
                .setExpiration(expirationEpochMs);
        when(watchMethod.execute()).thenReturn(response);

        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailSource result = gmailWatchService.registerWatch(EMAIL);

        assertNotNull(result.getWatchExpiration());
        // Expiration should be in the future
        assertTrue(result.getWatchExpiration().isAfter(ZonedDateTime.now()));
    }

    @Test
    public void registerWatch_setsWatchStatusActive() throws IOException {
        WatchResponse response = new WatchResponse()
                .setHistoryId(new BigInteger("22222222"))
                .setExpiration(System.currentTimeMillis() + 604800000L);
        when(watchMethod.execute()).thenReturn(response);

        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailSource result = gmailWatchService.registerWatch(EMAIL);

        assertEquals("ACTIVE", result.getWatchStatus());
    }
}

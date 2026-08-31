package com.placementos.backend.domain.service;

import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GmailNotificationHandlerTest {

    @Mock
    private GmailSourceRepository gmailSourceRepository;

    @InjectMocks
    private GmailNotificationHandler handler;

    private static final String EMAIL = "cdc@example.com";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private GmailSource sourceWith(String lastHistoryId) {
        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        source.setLastHistoryId(lastHistoryId);
        return source;
    }

    // --- Task 19 Test 10: missing emailAddress handled by controller, but test handler directly ---

    @Test
    public void handle_unregisteredEmailAddress_returnsSourceNotFound() {
        when(gmailSourceRepository.findByEmailAddress("unknown@example.com")).thenReturn(Optional.empty());

        GmailNotificationHandler.HandlerResult result = handler.handle("unknown@example.com", "12345");

        assertEquals(GmailNotificationHandler.HandlerResult.SOURCE_NOT_FOUND, result);
        verify(gmailSourceRepository, never()).save(any());
    }

    @Test
    public void handle_incomingHistoryIdNewer_advancesCursor() {
        GmailSource source = sourceWith("10000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "20000");

        assertEquals(GmailNotificationHandler.HandlerResult.CURSOR_ADVANCED, result);
        assertEquals("20000", source.getLastHistoryId());
        verify(gmailSourceRepository).save(source);
    }

    @Test
    public void handle_sameHistoryId_isIdempotentAlreadySeen() {
        GmailSource source = sourceWith("10000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10000");

        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result);
        // Cursor must not be overwritten with the same value — no save
        verify(gmailSourceRepository, never()).save(any());
    }

    @Test
    public void handle_olderHistoryId_doesNotReplaceCursor() {
        // Out-of-order delivery: stored cursor is newer than incoming
        GmailSource source = sourceWith("20000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10000");

        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result);
        // Stored cursor must remain "20000" — older value must not overwrite it
        assertEquals("20000", source.getLastHistoryId());
        verify(gmailSourceRepository, never()).save(any());
    }

    @Test
    public void handle_duplicatePubSubDelivery_isHarmlessAlreadySeen() {
        // Pub/Sub redelivers the same message (same historyId)
        GmailSource source = sourceWith("55555");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result1 = handler.handle(EMAIL, "55555");
        GmailNotificationHandler.HandlerResult result2 = handler.handle(EMAIL, "55555");

        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result1);
        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result2);
        verify(gmailSourceRepository, never()).save(any());
    }

    @Test
    public void handle_nullStoredHistoryId_treatsIncomingAsNewer() {
        // This is the state after watch registration when no cursor is yet stored
        // (first incoming notification — could be the initial watch confirmation)
        GmailSource source = sourceWith(null);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "12345");

        assertEquals(GmailNotificationHandler.HandlerResult.CURSOR_ADVANCED, result);
        assertEquals("12345", source.getLastHistoryId());
    }

    @Test
    public void handle_initialWatchNotification_advancesCursorWithoutFetchingEmails() {
        // When a watch is first registered, Google sends a synthetic initial notification.
        // This is a mailbox synchronization signal, NOT an indicator that a new email arrived.
        // The handler advances the cursor — it does NOT fetch email content regardless.
        GmailSource source = sourceWith(null);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "99001");

        assertEquals(GmailNotificationHandler.HandlerResult.CURSOR_ADVANCED, result);
        assertEquals("99001", source.getLastHistoryId());
        // No Gmail API calls, no email fetching — only cursor advancement
    }

    @Test
    public void handle_largeHistoryIds_comparedNumericallyNotLexicographically() {
        // "9" > "10" lexicographically but "9" < "10" numerically — must use numeric comparison
        GmailSource source = sourceWith("9");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));
        when(gmailSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10");

        // "10" is numerically greater than "9" — cursor should advance
        assertEquals(GmailNotificationHandler.HandlerResult.CURSOR_ADVANCED, result);
        assertEquals("10", source.getLastHistoryId());
    }
}

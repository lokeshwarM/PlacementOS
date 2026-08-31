package com.placementos.backend.domain.service;

import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailNotificationHandlerTest {

    @Mock
    private GmailSourceRepository gmailSourceRepository;

    @Mock
    private GmailHistoryService gmailHistoryService;

    @InjectMocks
    private GmailNotificationHandler handler;

    private static final String EMAIL = "cdc@example.com";

    private GmailSource sourceWith(String lastHistoryId) {
        GmailSource source = new GmailSource();
        source.setEmailAddress(EMAIL);
        source.setLastHistoryId(lastHistoryId);
        return source;
    }

    @Test
    public void handle_unregisteredEmailAddress_returnsSourceNotFound() {
        when(gmailSourceRepository.findByEmailAddress("unknown@example.com")).thenReturn(Optional.empty());

        GmailNotificationHandler.HandlerResult result = handler.handle("unknown@example.com", "12345");

        assertEquals(GmailNotificationHandler.HandlerResult.SOURCE_NOT_FOUND, result);
        verifyNoInteractions(gmailHistoryService);
    }

    @Test
    public void handle_incomingHistoryIdNewer_triggersSync() {
        GmailSource source = sourceWith("10000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "20000");

        assertEquals(GmailNotificationHandler.HandlerResult.SYNC_SUCCESS, result);
        verify(gmailHistoryService).syncHistory(EMAIL);
    }

    @Test
    public void handle_sameHistoryId_isIdempotentAlreadySeen() {
        GmailSource source = sourceWith("10000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10000");

        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result);
        verifyNoInteractions(gmailHistoryService);
    }

    @Test
    public void handle_olderHistoryId_doesNotTriggerSync() {
        // Out-of-order delivery: stored cursor is newer than incoming
        GmailSource source = sourceWith("20000");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10000");

        assertEquals(GmailNotificationHandler.HandlerResult.ALREADY_SEEN, result);
        verifyNoInteractions(gmailHistoryService);
    }

    @Test
    public void handle_nullStoredHistoryId_treatsIncomingAsNewer() {
        // This is the state after watch registration when no cursor is yet stored
        GmailSource source = sourceWith(null);
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "12345");

        assertEquals(GmailNotificationHandler.HandlerResult.SYNC_SUCCESS, result);
        verify(gmailHistoryService).syncHistory(EMAIL);
    }

    @Test
    public void handle_largeHistoryIds_comparedNumericallyNotLexicographically() {
        // "9" > "10" lexicographically but "9" < "10" numerically — must use numeric comparison
        GmailSource source = sourceWith("9");
        when(gmailSourceRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(source));

        GmailNotificationHandler.HandlerResult result = handler.handle(EMAIL, "10");

        assertEquals(GmailNotificationHandler.HandlerResult.SYNC_SUCCESS, result);
        verify(gmailHistoryService).syncHistory(EMAIL);
    }
}

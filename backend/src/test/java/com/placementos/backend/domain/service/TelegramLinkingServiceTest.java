package com.placementos.backend.domain.service;

import com.placementos.backend.config.TelegramProperties;
import com.placementos.backend.domain.dto.student.TelegramLinkResponse;
import com.placementos.backend.domain.dto.student.TelegramStatusResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.TelegramIdentity;
import com.placementos.backend.domain.entity.TelegramLinkToken;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramLinkTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramLinkingServiceTest {

    @Mock
    private TelegramLinkTokenRepository linkTokenRepository;

    @Mock
    private TelegramIdentityRepository identityRepository;

    private TelegramProperties telegramProperties;
    private TelegramLinkingService linkingService;
    private Student student;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setBotUsername("PlacementOS_bot");
        linkingService = new TelegramLinkingService(linkTokenRepository, identityRepository, telegramProperties);

        student = new Student();
        student.setId(1L);
        student.setName("Alice");
        student.setRegistrationNumber("RA2111003010001");
    }

    @Test
    void createLinkToken_generatesRandomSingleUseTokenWithExpiry() {
        TelegramLinkResponse response = linkingService.createLinkToken(student);

        assertNotNull(response.token());
        assertEquals(32, response.token().length());
        assertTrue(response.deepLink().startsWith("https://t.me/PlacementOS_bot?start="));
        assertTrue(response.expiresAt().isAfter(Instant.now()));

        ArgumentCaptor<TelegramLinkToken> captor = ArgumentCaptor.forClass(TelegramLinkToken.class);
        verify(linkTokenRepository).save(captor.capture());

        TelegramLinkToken savedToken = captor.getValue();
        assertEquals(student, savedToken.getStudent());
        assertNotNull(savedToken.getTokenHash());
        assertNotEquals(response.token(), savedToken.getTokenHash()); // Hashed, never stored raw!
    }

    @Test
    void verifyAndLink_validToken_linksStudentAndMarksUsed() {
        TelegramLinkResponse linkResponse = linkingService.createLinkToken(student);

        ArgumentCaptor<TelegramLinkToken> captor = ArgumentCaptor.forClass(TelegramLinkToken.class);
        verify(linkTokenRepository).save(captor.capture());
        TelegramLinkToken savedToken = captor.getValue();

        when(linkTokenRepository.findByTokenHash(savedToken.getTokenHash()))
                .thenReturn(Optional.of(savedToken));
        when(identityRepository.findByTelegramChatId(999888777L))
                .thenReturn(Optional.empty());
        when(identityRepository.findByStudentId(1L))
                .thenReturn(Optional.empty());

        Student linked = linkingService.verifyAndLink(linkResponse.token(), 999888777L, 12345L, "alice_tele");

        assertEquals(student, linked);
        assertTrue(savedToken.isUsed());
        assertNotNull(savedToken.getUsedAt());

        verify(identityRepository).save(argThat(identity ->
                identity.getStudent().equals(student) &&
                identity.getTelegramChatId().equals(999888777L) &&
                identity.getTelegramUserId().equals(12345L) &&
                "alice_tele".equals(identity.getTelegramUsername())
        ));
    }

    @Test
    void verifyAndLink_expiredToken_throwsIllegalStateException() {
        TelegramLinkToken expiredToken = new TelegramLinkToken("hash-1", student, Instant.now().minusSeconds(60));
        when(linkTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                linkingService.verifyAndLink("token-1", 999L, 111L, "user"));

        assertTrue(ex.getMessage().contains("expired"));
        verify(identityRepository, never()).save(any());
    }

    @Test
    void verifyAndLink_alreadyUsedToken_throwsIllegalStateException() {
        TelegramLinkToken usedToken = new TelegramLinkToken("hash-2", student, Instant.now().plusSeconds(600));
        usedToken.setUsedAt(Instant.now().minusSeconds(10));
        when(linkTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(usedToken));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                linkingService.verifyAndLink("token-2", 999L, 111L, "user"));

        assertTrue(ex.getMessage().contains("already been used"));
        verify(identityRepository, never()).save(any());
    }

    @Test
    void verifyAndLink_chatAlreadyClaimedByAnotherStudent_throwsConflict() {
        TelegramLinkToken token = new TelegramLinkToken("hash-3", student, Instant.now().plusSeconds(600));
        when(linkTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        Student otherStudent = new Student();
        otherStudent.setId(2L);
        TelegramIdentity existingIdentity = new TelegramIdentity(otherStudent, 999L, 111L, "other");

        when(identityRepository.findByTelegramChatId(999L)).thenReturn(Optional.of(existingIdentity));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                linkingService.verifyAndLink("token-3", 999L, 111L, "user"));

        assertTrue(ex.getMessage().contains("already linked to another student"));
        verify(identityRepository, never()).save(any());
    }

    @Test
    void unlink_removesIdentity() {
        TelegramIdentity identity = new TelegramIdentity(student, 999L, 111L, "alice");
        when(identityRepository.findByStudentId(1L)).thenReturn(Optional.of(identity));

        linkingService.unlink(student);

        verify(identityRepository).delete(identity);
    }

    @Test
    void getStatus_returnsCorrectStatus() {
        when(identityRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        TelegramStatusResponse notLinked = linkingService.getStatus(student);
        assertFalse(notLinked.linked());

        TelegramIdentity identity = new TelegramIdentity(student, 999L, 111L, "alice");
        identity.setLinkedAt(Instant.now());
        when(identityRepository.findByStudentId(1L)).thenReturn(Optional.of(identity));

        TelegramStatusResponse linked = linkingService.getStatus(student);
        assertTrue(linked.linked());
        assertEquals("alice", linked.telegramUsername());
        assertNotNull(linked.linkedAt());
    }
}

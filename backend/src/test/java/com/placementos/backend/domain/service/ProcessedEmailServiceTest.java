package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.ProcessedEmail;
import com.placementos.backend.domain.enums.EmailProcessingStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.ProcessedEmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProcessedEmailService} — idempotency boundary checks.
 */
@ExtendWith(MockitoExtension.class)
class ProcessedEmailServiceTest {

    @Mock
    ProcessedEmailRepository processedEmailRepository;

    @InjectMocks
    ProcessedEmailService processedEmailService;

    @Test
    void isDuplicate_returnsTrueWhenAlreadyProcessed() {
        when(processedEmailRepository.existsByMessageId("MSG-123")).thenReturn(true);

        assertThat(processedEmailService.isDuplicate("MSG-123")).isTrue();
    }

    @Test
    void isDuplicate_returnsFalseForNewMessage() {
        when(processedEmailRepository.existsByMessageId("MSG-NEW")).thenReturn(false);

        assertThat(processedEmailService.isDuplicate("MSG-NEW")).isFalse();
    }

    @Test
    void registerProcessed_success() {
        when(processedEmailRepository.existsByMessageId("MSG-ABC")).thenReturn(false);

        ProcessedEmail saved = new ProcessedEmail();
        saved.setMessageId("MSG-ABC");
        saved.setProcessingStatus(EmailProcessingStatus.PROCESSED);
        when(processedEmailRepository.save(any(ProcessedEmail.class))).thenReturn(saved);

        ProcessedEmail result = processedEmailService.registerProcessed(
                "MSG-ABC", "THREAD-001", "inbox@vit.ac.in", Instant.now());

        assertThat(result.getMessageId()).isEqualTo("MSG-ABC");
        assertThat(result.getProcessingStatus()).isEqualTo(EmailProcessingStatus.PROCESSED);
    }

    @Test
    void registerProcessed_duplicate_throwsDuplicate() {
        when(processedEmailRepository.existsByMessageId("MSG-DUP")).thenReturn(true);

        assertThatThrownBy(() -> processedEmailService.registerProcessed(
                "MSG-DUP", null, null, Instant.now()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("MSG-DUP");

        verify(processedEmailRepository, never()).save(any());
    }
}

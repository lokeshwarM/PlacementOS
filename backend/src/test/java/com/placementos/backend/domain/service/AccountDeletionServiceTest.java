package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.UserRole;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.NotificationOutboxRepository;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramLinkTokenRepository;
import com.placementos.backend.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private TelegramIdentityRepository telegramIdentityRepository;

    @Mock
    private TelegramLinkTokenRepository telegramLinkTokenRepository;

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    private UserAccount testUser;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testUser = new UserAccount("student@example.com", "$2a$10$hashedPassword", UserRole.STUDENT);
        testUser.setId(42L);

        testStudent = new Student();
        testStudent.setId(10L);
        testStudent.setRegistrationNumber("REG12345");
        testStudent.setNeopatId("NEO999");
        testStudent.setName("Alice Bob");
        testStudent.setBranch("Computer Science");
        testStudent.setBatch(2025);
        testStudent.setCgpa(new BigDecimal("8.75"));
        testStudent.setPhoneNumber("+919876543210");
        testStudent.setGender("FEMALE");
        testStudent.setSpecialization("AI/ML");
        testUser.setStudent(testStudent);
    }

    @Test
    @DisplayName("deleteAccount successfully anonymises PII, unlinks Telegram, cancels reminders/outbox, and retains institutional records")
    void testDeleteAccountSuccess() {
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(Optional.of(testUser));
        when(notificationOutboxRepository.cancelPendingByStudentId(eq(10L), eq("ACCOUNT_DELETED"), any(Instant.class)))
                .thenReturn(3);

        accountDeletionService.deleteAccount("student@example.com");

        // 1. Reminders cancelled
        verify(reminderService).cancelAllRemindersForStudent(10L, "ACCOUNT_DELETED");

        // 2. Outbox cancelled
        verify(notificationOutboxRepository).cancelPendingByStudentId(eq(10L), eq("ACCOUNT_DELETED"), any(Instant.class));

        // 3. Telegram unlinked
        verify(telegramLinkTokenRepository).deleteByStudentId(10L);
        verify(telegramIdentityRepository).deleteByStudentId(10L);

        // 4. Student PII anonymised
        assertThat(testStudent.getName()).isEqualTo("DELETED");
        assertThat(testStudent.getPhoneNumber()).isNull();
        assertThat(testStudent.getGender()).isNull();
        assertThat(testStudent.getSpecialization()).isNull();
        // Institutional fields must be preserved
        assertThat(testStudent.getRegistrationNumber()).isEqualTo("REG12345");
        assertThat(testStudent.getNeopatId()).isEqualTo("NEO999");
        assertThat(testStudent.getBranch()).isEqualTo("Computer Science");
        assertThat(testStudent.getBatch()).isEqualTo(2025);
        assertThat(testStudent.getCgpa()).isEqualByComparingTo("8.75");

        // 5. User credentials anonymised
        assertThat(testUser.getEmail()).isEqualTo("deleted_42@placementos.invalid");
        assertThat(testUser.getPasswordHash()).startsWith("$2a$10$DELETED");
        assertThat(testUser.getDeletedAt()).isNotNull();
        assertThat(testUser.getAnonymizedAt()).isNotNull();
        assertThat(testUser.isDeleted()).isTrue();

        verify(userAccountRepository).save(testUser);
    }

    @Test
    @DisplayName("deleteAccount is idempotent when called on already deleted account")
    void testDeleteAccountIdempotency() {
        testUser.setDeletedAt(Instant.now());
        when(userAccountRepository.findByEmail("deleted_42@placementos.invalid")).thenReturn(Optional.of(testUser));

        accountDeletionService.deleteAccount("deleted_42@placementos.invalid");

        // Should not repeat operations
        verifyNoInteractions(reminderService);
        verifyNoInteractions(telegramIdentityRepository);
        verifyNoInteractions(telegramLinkTokenRepository);
        verifyNoInteractions(notificationOutboxRepository);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAccount handles user without linked student record")
    void testDeleteAccountWithoutStudent() {
        testUser.setStudent(null);
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(Optional.of(testUser));

        accountDeletionService.deleteAccount("student@example.com");

        verifyNoInteractions(reminderService);
        verifyNoInteractions(telegramIdentityRepository);
        verifyNoInteractions(telegramLinkTokenRepository);
        verifyNoInteractions(notificationOutboxRepository);

        assertThat(testUser.getEmail()).isEqualTo("deleted_42@placementos.invalid");
        assertThat(testUser.isDeleted()).isTrue();
        verify(userAccountRepository).save(testUser);
    }

    @Test
    @DisplayName("deleteAccount throws ResourceNotFoundException if account does not exist")
    void testDeleteAccountNotFound() {
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountDeletionService.deleteAccount("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }
}

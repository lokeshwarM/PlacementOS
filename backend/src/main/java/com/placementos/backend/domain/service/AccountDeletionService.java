package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.NotificationOutboxRepository;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramLinkTokenRepository;
import com.placementos.backend.domain.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Handles authenticated account deletion.
 *
 * <h3>Deletion semantics:</h3>
 * <ol>
 *   <li><strong>Cancelled:</strong> Active reminder tasks, pending outbox notifications.</li>
 *   <li><strong>Removed:</strong> Telegram identity and all link tokens for the student.</li>
 *   <li><strong>Anonymised:</strong> Student PII: name → "DELETED", phone_number → null,
 *       gender → null, specialization → null. CGPA/batch/branch retained for
 *       eligibility audit. registration_number and neopat_id retained as institutional
 *       identifiers used for historical shortlist matching.</li>
 *   <li><strong>Anonymised:</strong> UserAccount email → "deleted_{id}@placementos.invalid",
 *       password_hash → random BCrypt placeholder, profile_status unchanged,
 *       deleted_at and anonymized_at set.</li>
 *   <li><strong>Retained (NOT deleted):</strong> PlacementDrive, Application, ShortlistEntry,
 *       StudentEligibilityResult, Notification, NotificationOutbox (cancelled, not deleted).
 *       These belong to the university system of record.</li>
 * </ol>
 *
 * <h3>Security:</h3>
 * The target account is always derived from the authenticated principal.
 * No client-supplied userId or email is accepted.
 *
 * <h3>Idempotency:</h3>
 * Calling this method on an already-deleted account is safe — it returns without error.
 */
@Service
@Transactional
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserAccountRepository userAccountRepository;
    private final ReminderService reminderService;
    private final TelegramIdentityRepository telegramIdentityRepository;
    private final TelegramLinkTokenRepository telegramLinkTokenRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    public AccountDeletionService(UserAccountRepository userAccountRepository,
                                  ReminderService reminderService,
                                  TelegramIdentityRepository telegramIdentityRepository,
                                  TelegramLinkTokenRepository telegramLinkTokenRepository,
                                  NotificationOutboxRepository notificationOutboxRepository) {
        this.userAccountRepository = userAccountRepository;
        this.reminderService = reminderService;
        this.telegramIdentityRepository = telegramIdentityRepository;
        this.telegramLinkTokenRepository = telegramLinkTokenRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
    }

    /**
     * Deletes the account associated with the given email (derived from authenticated principal).
     * Does not accept arbitrary email from the client.
     *
     * @param email the authenticated user's email (from JWT principal)
     */
    public void deleteAccount(String email) {
        Objects.requireNonNull(email, "email must not be null");

        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Idempotent: already deleted → nothing to do
        if (user.isDeleted()) {
            log.info("Account id={} already deleted — skipping.", user.getId());
            return;
        }

        Instant now = Instant.now();
        Student student = user.getStudent();

        // ── 1. Cancel all active reminders ──────────────────────────────────
        if (student != null) {
            try {
                reminderService.cancelAllRemindersForStudent(student.getId(), "ACCOUNT_DELETED");
                log.info("Cancelled all reminder tasks for student id={}", student.getId());
            } catch (Exception e) {
                log.warn("Failed to cancel reminders for student id={}: {}", student.getId(), e.getMessage());
            }
        }

        // ── 2. Cancel all pending outbox notifications ───────────────────────
        if (student != null) {
            try {
                int cancelled = notificationOutboxRepository.cancelPendingByStudentId(student.getId(), "ACCOUNT_DELETED", now);
                log.info("Cancelled {} pending outbox entries for student id={}", cancelled, student.getId());
            } catch (Exception e) {
                log.warn("Failed to cancel outbox for student id={}: {}", student.getId(), e.getMessage());
            }
        }

        // ── 3. Remove Telegram identity & link tokens ────────────────────────
        if (student != null) {
            try {
                telegramLinkTokenRepository.deleteByStudentId(student.getId());
                telegramIdentityRepository.deleteByStudentId(student.getId());
                log.info("Removed Telegram identity and link tokens for student id={}", student.getId());
            } catch (Exception e) {
                log.warn("Failed to remove Telegram data for student id={}: {}", student.getId(), e.getMessage());
            }
        }

        // ── 4. Anonymise Student PII ─────────────────────────────────────────
        if (student != null) {
            anonymiseStudentPii(student, now);
            log.info("Anonymised PII for student id={}", student.getId());
        }

        // ── 5. Anonymise UserAccount authentication credentials ──────────────
        user.setEmail("deleted_" + user.getId() + "@placementos.invalid");
        user.setPasswordHash("$2a$10$DELETEDACCOUNTPLACEHOLDERHASHNOREENTRYXX"); // invalid BCrypt
        user.setDeletedAt(now);
        user.setAnonymizedAt(now);
        userAccountRepository.save(user);

        log.info("Account id={} successfully deleted and anonymised.", user.getId());
    }

    /**
     * Anonymises the mutable personal fields of the student.
     * Retains: registration_number, neopat_id, branch, batch, cgpa, degree
     * (institutional identifiers and academic facts required for historical shortlist integrity).
     */
    private void anonymiseStudentPii(Student student, Instant deletedAt) {
        student.setName("DELETED");
        student.setPhoneNumber(null);
        student.setGender(null);
        student.setSpecialization(null);
        // registration_number, neopat_id, branch, batch, cgpa, degree: intentionally retained
    }
}

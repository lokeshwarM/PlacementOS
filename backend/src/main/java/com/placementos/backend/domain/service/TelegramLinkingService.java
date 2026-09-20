package com.placementos.backend.domain.service;

import com.placementos.backend.config.TelegramProperties;
import com.placementos.backend.domain.dto.student.TelegramLinkResponse;
import com.placementos.backend.domain.dto.student.TelegramStatusResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.TelegramIdentity;
import com.placementos.backend.domain.entity.TelegramLinkToken;
import com.placementos.backend.domain.repository.TelegramIdentityRepository;
import com.placementos.backend.domain.repository.TelegramLinkTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TelegramLinkingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramLinkingService.class);
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TelegramLinkTokenRepository linkTokenRepository;
    private final TelegramIdentityRepository identityRepository;
    private final TelegramProperties telegramProperties;

    public TelegramLinkingService(TelegramLinkTokenRepository linkTokenRepository,
                                  TelegramIdentityRepository identityRepository,
                                  TelegramProperties telegramProperties) {
        this.linkTokenRepository = linkTokenRepository;
        this.identityRepository = identityRepository;
        this.telegramProperties = telegramProperties;
    }

    /**
     * Generates a single-use cryptographically random linking token for the authenticated student.
     * Persists the SHA-256 hash of the token to prevent plaintext token leaks.
     */
    @Transactional
    public TelegramLinkResponse createLinkToken(Student student) {
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plus(TOKEN_VALIDITY);
        TelegramLinkToken linkToken = new TelegramLinkToken(tokenHash, student, expiresAt);
        linkTokenRepository.save(linkToken);

        String botUsername = telegramProperties.getBotUsername() != null ? telegramProperties.getBotUsername() : "PlacementOS_bot";
        String deepLink = String.format("https://t.me/%s?start=%s", botUsername, rawToken);

        log.info("Generated Telegram link token for student id={}, expiresAt={}", student.getId(), expiresAt);
        return new TelegramLinkResponse(rawToken, deepLink, expiresAt);
    }

    /**
     * Gets the current Telegram connection status for the student.
     */
    public TelegramStatusResponse getStatus(Student student) {
        return identityRepository.findByStudentId(student.getId())
                .map(identity -> new TelegramStatusResponse(true, identity.getTelegramUsername(), identity.getLinkedAt()))
                .orElseGet(() -> new TelegramStatusResponse(false, null, null));
    }

    /**
     * Unlinks the Telegram identity for the authenticated student.
     */
    @Transactional
    public void unlink(Student student) {
        identityRepository.findByStudentId(student.getId()).ifPresent(identity -> {
            identityRepository.delete(identity);
            log.info("Unlinked Telegram identity for student id={}", student.getId());
        });
    }

    /**
     * Validates a one-time token from a Telegram /start command and securely associates
     * the Telegram chat ID and user identity with the student.
     */
    @Transactional
    public Student verifyAndLink(String rawToken, Long chatId, Long userId, String username) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Linking token cannot be blank");
        }

        String tokenHash = hashToken(rawToken.trim());
        TelegramLinkToken linkToken = linkTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or non-existent linking token"));

        if (linkToken.isExpired()) {
            throw new IllegalStateException("Linking token has expired. Please generate a new link in PlacementOS.");
        }

        if (linkToken.isUsed()) {
            throw new IllegalStateException("Linking token has already been used.");
        }

        // Verify that this chatId is not already linked to another student
        Optional<TelegramIdentity> existingChatLink = identityRepository.findByTelegramChatId(chatId);
        if (existingChatLink.isPresent() && !existingChatLink.get().getStudent().getId().equals(linkToken.getStudent().getId())) {
            log.warn("Telegram linking conflict: chatId={} is already claimed by student id={}",
                    chatId, existingChatLink.get().getStudent().getId());
            throw new IllegalStateException("This Telegram account is already linked to another student.");
        }

        Student student = linkToken.getStudent();

        // Create or update TelegramIdentity
        TelegramIdentity identity = identityRepository.findByStudentId(student.getId())
                .orElseGet(() -> new TelegramIdentity(student, chatId, userId, username));

        identity.setTelegramChatId(chatId);
        identity.setTelegramUserId(userId);
        identity.setTelegramUsername(username);
        identity.setLinkedAt(Instant.now());

        identityRepository.save(identity);

        // Mark token as used
        linkToken.setUsedAt(Instant.now());
        linkTokenRepository.save(linkToken);

        log.info("Successfully linked Telegram chatId={} (user={}, username={}) to student id={}",
                chatId, userId, username, student.getId());

        return student;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

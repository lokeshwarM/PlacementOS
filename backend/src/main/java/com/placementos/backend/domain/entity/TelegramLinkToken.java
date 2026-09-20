package com.placementos.backend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for short-lived, single-use Telegram linking tokens.
 * Only the SHA-256 hash of the token is stored for security.
 */
@Entity
@Table(
    name = "telegram_link_tokens",
    indexes = {
        @Index(name = "idx_telegram_link_tokens_student_id", columnList = "student_id"),
        @Index(name = "idx_telegram_link_tokens_expires_at", columnList = "expires_at")
    }
)
public class TelegramLinkToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant expiresAt;

    @Column(name = "used_at", columnDefinition = "TIMESTAMPTZ")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    public TelegramLinkToken() {}

    public TelegramLinkToken(String tokenHash, Student student, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.student = student;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelegramLinkToken other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.TelegramLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, Long> {

    Optional<TelegramLinkToken> findByTokenHash(String tokenHash);

    List<TelegramLinkToken> findByStudentId(Long studentId);

    void deleteByExpiresAtBefore(Instant now);
}

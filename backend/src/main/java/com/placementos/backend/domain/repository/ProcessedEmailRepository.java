package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Persistence repository for {@link ProcessedEmail}.
 * The primary contract is the idempotency check on message_id.
 */
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {

    /**
     * Checks whether a Gmail message has already been ingested.
     * This must be called before beginning any processing of an incoming email.
     */
    boolean existsByMessageId(String messageId);

    Optional<ProcessedEmail> findByMessageId(String messageId);
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.GmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GmailMessageRepository extends JpaRepository<GmailMessage, Long> {

    Optional<GmailMessage> findByGmailSourceIdAndMessageId(Long gmailSourceId, String messageId);

    boolean existsByGmailSourceIdAndMessageId(Long gmailSourceId, String messageId);

    Optional<GmailMessage> findByMessageId(String messageId);

    List<GmailMessage> findByGmailSourceId(Long gmailSourceId);
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.TelegramWebhookUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramWebhookUpdateRepository extends JpaRepository<TelegramWebhookUpdate, Long> {

    boolean existsByUpdateId(Long updateId);
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.TelegramIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramIdentityRepository extends JpaRepository<TelegramIdentity, Long> {

    Optional<TelegramIdentity> findByStudentId(Long studentId);

    Optional<TelegramIdentity> findByTelegramChatId(Long telegramChatId);

    Optional<TelegramIdentity> findByTelegramUserId(Long telegramUserId);

    boolean existsByTelegramChatId(Long telegramChatId);

    boolean existsByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);
}

package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Persistence repository for {@link UserAccount}.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserAccount> findByStudentId(Long studentId);

    boolean existsByStudentId(Long studentId);
}

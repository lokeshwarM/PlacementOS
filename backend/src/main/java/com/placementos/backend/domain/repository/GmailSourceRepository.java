package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.model.GmailSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GmailSourceRepository extends JpaRepository<GmailSource, Long> {
    Optional<GmailSource> findByEmailAddress(String emailAddress);
}

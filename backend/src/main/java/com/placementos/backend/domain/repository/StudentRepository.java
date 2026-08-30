package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Persistence repository for {@link Student}.
 * Primary lookup keys: registration_number and neopat_id.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRegistrationNumber(String registrationNumber);

    Optional<Student> findByNeopatId(String neopatId);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByNeopatId(String neopatId);
}

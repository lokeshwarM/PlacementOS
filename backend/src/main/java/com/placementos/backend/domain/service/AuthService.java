package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.auth.AuthResponse;
import com.placementos.backend.domain.dto.auth.LoginRequest;
import com.placementos.backend.domain.dto.auth.RegisterRequest;
import com.placementos.backend.domain.dto.auth.UserAccountResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.UserRole;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.StudentRepository;
import com.placementos.backend.domain.repository.UserAccountRepository;
import com.placementos.backend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Service managing user authentication, registration, password hashing, and token issuance.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserAccountRepository userAccountRepository,
                       StudentRepository studentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new student user account. Always assigns STUDENT role.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration failed: Account already exists for email {}", normalizedEmail);
            throw new DuplicateResourceException("An account with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserAccount user = new UserAccount(normalizedEmail, passwordHash, UserRole.STUDENT);
        UserAccount savedUser = userAccountRepository.save(user);

        log.info("Registered new user account id={} email={}", savedUser.getId(), normalizedEmail);

        String token = jwtTokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                null
        );

        return AuthResponse.of(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getProfileStatus(),
                null,
                null
        );
    }

    /**
     * Authenticates user credentials and returns a JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user email {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        Student student = user.getStudent();
        Long studentId = student != null ? student.getId() : null;
        String studentName = student != null ? student.getName() : null;

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                studentId
        );

        log.info("User id={} logged in successfully", user.getId());

        return AuthResponse.of(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getProfileStatus(),
                studentId,
                studentName
        );
    }

    /**
     * Retrieves current user account information.
     */
    public UserAccountResponse getCurrentUser(String email) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserAccountResponse.from(user);
    }
}

package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.auth.AuthResponse;
import com.placementos.backend.domain.dto.auth.LoginRequest;
import com.placementos.backend.domain.dto.auth.RegisterRequest;
import com.placementos.backend.domain.dto.auth.UserAccountResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.enums.ProfileStatus;
import com.placementos.backend.domain.enums.UserRole;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.StudentRepository;
import com.placementos.backend.domain.repository.UserAccountRepository;
import com.placementos.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private UserAccount sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new UserAccount("alice@example.com", "encodedPassword", UserRole.STUDENT);
        sampleUser.setId(100L);
    }

    @Test
    @DisplayName("register creates new STUDENT user account and returns JWT token")
    void register_success() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "secret123");

        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken(100L, "alice@example.com", UserRole.STUDENT, null))
                .thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        assertThat(response.profileStatus()).isEqualTo(ProfileStatus.INCOMPLETE);
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("register throws DuplicateResourceException on existing email")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "secret123");
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("login validates password and returns token with linked student details")
    void login_success() {
        Student student = new Student();
        student.setId(500L);
        student.setName("Alice Smith");
        sampleUser.setStudent(student);
        sampleUser.setProfileStatus(ProfileStatus.COMPLETE);

        LoginRequest request = new LoginRequest("alice@example.com", "secret123");

        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(100L, "alice@example.com", UserRole.STUDENT, 500L))
                .thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.studentId()).isEqualTo(500L);
        assertThat(response.studentName()).isEqualTo("Alice Smith");
        assertThat(response.profileStatus()).isEqualTo(ProfileStatus.COMPLETE);
    }

    @Test
    @DisplayName("login throws BadCredentialsException on password mismatch")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrongPassword");

        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login throws BadCredentialsException for unknown email")
    void login_unknownEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "secret123");
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("getCurrentUser returns user account details")
    void getCurrentUser_success() {
        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));

        UserAccountResponse response = authService.getCurrentUser("alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
    }
}

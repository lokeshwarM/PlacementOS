package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.ProfileStatus;
import com.placementos.backend.domain.enums.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity mapping the {@code users} table.
 * Encapsulates authentication credentials and maps 1-to-1 to a domain {@link Student}.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uq_users_student_id", columnNames = {"student_id"})
    }
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role = UserRole.STUDENT;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 50)
    private ProfileStatus profileStatus = ProfileStatus.INCOMPLETE;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    /** Set when the account is soft-deleted. Null means the account is active. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Set when student PII has been anonymised. */
    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (role == null) role = UserRole.STUDENT;
        if (profileStatus == null) profileStatus = ProfileStatus.INCOMPLETE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UserAccount() {}

    public UserAccount(String email, String passwordHash, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.STUDENT;
        this.profileStatus = ProfileStatus.INCOMPLETE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public ProfileStatus getProfileStatus() { return profileStatus; }
    public void setProfileStatus(ProfileStatus profileStatus) { this.profileStatus = profileStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Instant getAnonymizedAt() { return anonymizedAt; }
    public void setAnonymizedAt(Instant anonymizedAt) { this.anonymizedAt = anonymizedAt; }

    public boolean isDeleted() { return deletedAt != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserAccount{id=" + id + ", email='" + email + '\'' + ", role=" + role + ", profileStatus=" + profileStatus + "}";
    }
}

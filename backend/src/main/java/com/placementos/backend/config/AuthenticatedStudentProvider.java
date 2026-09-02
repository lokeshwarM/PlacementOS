package com.placementos.backend.config;

import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.entity.UserAccount;
import com.placementos.backend.domain.repository.StudentRepository;
import com.placementos.backend.domain.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * Component providing secure, principal-derived student identity resolution.
 * Prevents parameter-based impersonation on student-facing endpoints.
 */
@Component
public class AuthenticatedStudentProvider {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedStudentProvider.class);

    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;

    public AuthenticatedStudentProvider(StudentRepository studentRepository,
                                       UserAccountRepository userAccountRepository) {
        this.studentRepository = studentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Resolves the currently authenticated student from Spring Security's SecurityContext.
     *
     * @return Resolved Student entity
     * @throws AccessDeniedException if not authenticated or no matching student exists
     */
    public Student getAuthenticatedStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String principalName = auth.getName();
        return resolveStudentByPrincipalName(principalName)
                .orElseThrow(() -> new AccessDeniedException("No student record linked to authenticated user: " + principalName));
    }

    /**
     * Resolves student from an explicit Principal object (injected into controller methods).
     */
    public Student getStudentFromPrincipal(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank() || "anonymousUser".equals(principal.getName())) {
            return getAuthenticatedStudent();
        }

        return resolveStudentByPrincipalName(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("No student record linked to principal: " + principal.getName()));
    }

    /**
     * Resolves user account from an explicit Principal object.
     */
    public UserAccount getUserAccountFromPrincipal(Principal principal) {
        String identifier = (principal != null && principal.getName() != null) ? principal.getName() : null;
        if (identifier == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                identifier = auth.getName();
            }
        }

        if (identifier == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userAccountRepository.findByEmail(identifier)
                .orElseThrow(() -> new AccessDeniedException("No user account found for principal: " + identifier));
    }

    /**
     * Verifies that the authenticated student owns the resource.
     */
    public void verifyOwnership(Student authenticatedStudent, Long resourceOwnerStudentId) {
        if (authenticatedStudent == null || resourceOwnerStudentId == null) {
            throw new AccessDeniedException("Invalid authorization state for ownership verification");
        }

        if (!authenticatedStudent.getId().equals(resourceOwnerStudentId)) {
            log.warn("Access denied: Authenticated student id={} attempted to access resource owned by student id={}",
                    authenticatedStudent.getId(), resourceOwnerStudentId);
            throw new AccessDeniedException("Access denied: You cannot access or modify resources belonging to another student");
        }
    }

    private Optional<Student> resolveStudentByPrincipalName(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        // 1. First check if it's a UserAccount email with a linked Student
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(identifier);
        if (userOpt.isPresent() && userOpt.get().getStudent() != null) {
            return Optional.of(userOpt.get().getStudent());
        }

        // 2. Try by numeric student ID (for test mocks / internal callers)
        try {
            Long id = Long.parseLong(identifier);
            Optional<Student> studentById = studentRepository.findById(id);
            if (studentById.isPresent()) return studentById;
        } catch (NumberFormatException ignored) {
            // Not a numeric ID
        }

        // 3. Try by registration number
        Optional<Student> studentByReg = studentRepository.findByRegistrationNumber(identifier);
        if (studentByReg.isPresent()) return studentByReg;

        // 4. Try by NeoPAT ID
        return studentRepository.findByNeopatId(identifier);
    }
}

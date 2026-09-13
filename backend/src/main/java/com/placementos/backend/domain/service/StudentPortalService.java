package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.student.*;
import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.*;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating the student portal dashboard, role-aware placement views,
 * profile onboarding, applications, notifications, and reminders.
 */
@Service
@Transactional(readOnly = true)
public class StudentPortalService {

    private static final Logger log = LoggerFactory.getLogger(StudentPortalService.class);

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRoleRepository placementRoleRepository;
    private final StudentEligibilityResultRepository eligibilityResultRepository;
    private final ApplicationRepository applicationRepository;
    private final ShortlistEntryRepository shortlistEntryRepository;
    private final NotificationRepository notificationRepository;
    private final ReminderTaskRepository reminderTaskRepository;

    public StudentPortalService(UserAccountRepository userAccountRepository,
                                StudentRepository studentRepository,
                                PlacementDriveRepository placementDriveRepository,
                                PlacementRoleRepository placementRoleRepository,
                                StudentEligibilityResultRepository eligibilityResultRepository,
                                ApplicationRepository applicationRepository,
                                ShortlistEntryRepository shortlistEntryRepository,
                                NotificationRepository notificationRepository,
                                ReminderTaskRepository reminderTaskRepository) {
        this.userAccountRepository = userAccountRepository;
        this.studentRepository = studentRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.placementRoleRepository = placementRoleRepository;
        this.eligibilityResultRepository = eligibilityResultRepository;
        this.applicationRepository = applicationRepository;
        this.shortlistEntryRepository = shortlistEntryRepository;
        this.notificationRepository = notificationRepository;
        this.reminderTaskRepository = reminderTaskRepository;
    }

    // -------------------------------------------------------------------------
    // Profile & Onboarding
    // -------------------------------------------------------------------------

    public StudentProfileResponse getStudentProfile(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return StudentProfileResponse.from(user);
    }

    @Transactional
    public StudentProfileResponse onboardStudent(Long userId, StudentOnboardingRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Student student = user.getStudent();
        String regNo = request.getRegistrationNumber().trim().toUpperCase();

        if (student == null) {
            Optional<Student> existingStudentOpt = studentRepository.findByRegistrationNumber(regNo);
            if (existingStudentOpt.isPresent()) {
                Student existingStudent = existingStudentOpt.get();
                Optional<UserAccount> existingLinkedUser = userAccountRepository.findByStudentId(existingStudent.getId());
                if (existingLinkedUser.isPresent() && !existingLinkedUser.get().getId().equals(user.getId())) {
                    log.warn("Onboarding conflict: Registration number {} is already linked to user {}", regNo, existingLinkedUser.get().getId());
                    throw new DuplicateResourceException("This registration number is already linked to another account");
                }
                student = existingStudent;
            } else {
                student = new Student();
            }
        }

        student.setName(request.getName().trim());
        student.setRegistrationNumber(regNo);
        student.setNeopatId(request.getNeopatId() != null ? request.getNeopatId().trim() : null);
        student.setBranch(request.getBranch().trim().toUpperCase());
        student.setBatch(request.getBatch());
        student.setCgpa(request.getCgpa());
        student.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null);
        student.setDegree(request.getDegree().trim());
        student.setSpecialization(request.getSpecialization() != null ? request.getSpecialization().trim() : null);
        student.setStandingArrears(request.getStandingArrears());
        student.setGender(request.getGender() != null ? request.getGender().trim().toUpperCase() : null);

        Student savedStudent = studentRepository.save(student);
        user.setStudent(savedStudent);

        if (isProfileComplete(savedStudent)) {
            user.setProfileStatus(ProfileStatus.COMPLETE);
        } else {
            user.setProfileStatus(ProfileStatus.INCOMPLETE);
        }

        UserAccount savedUser = userAccountRepository.save(user);
        log.info("Student onboarding completed for user id={} student id={}", savedUser.getId(), savedStudent.getId());
        return StudentProfileResponse.from(savedUser);
    }

    @Transactional
    public StudentProfileResponse updateStudentProfile(Long userId, StudentProfileUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Student student = user.getStudent();
        if (student == null) {
            throw new ResourceNotFoundException("Student profile not found. Please complete onboarding.");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            student.setName(request.getName().trim());
        }
        if (request.getPhoneNumber() != null) {
            student.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getSpecialization() != null) {
            student.setSpecialization(request.getSpecialization().trim());
        }
        if (request.getStandingArrears() != null) {
            student.setStandingArrears(request.getStandingArrears());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender().trim().toUpperCase());
        }

        studentRepository.save(student);
        return StudentProfileResponse.from(user);
    }

    // -------------------------------------------------------------------------
    // Placement Drives & Role-Specific Experience
    // -------------------------------------------------------------------------

    public PageResponse<StudentPlacementDriveCardResponse> getStudentPlacements(Long studentId, Pageable pageable) {
        Page<PlacementDrive> drivesPage = placementDriveRepository.findAll(pageable);
        List<StudentPlacementDriveCardResponse> cards = drivesPage.getContent().stream()
                .map(drive -> mapDriveToCard(studentId, drive))
                .toList();

        return new PageResponse<>(
                cards,
                drivesPage.getNumber(),
                drivesPage.getSize(),
                drivesPage.getTotalElements(),
                drivesPage.getTotalPages(),
                drivesPage.isLast()
        );
    }

    public StudentPlacementDetailResponse getStudentPlacementDetail(Long studentId, Long driveId) {
        PlacementDrive drive = placementDriveRepository.findById(driveId)
                .orElseThrow(() -> ResourceNotFoundException.placementDrive(driveId));

        List<PlacementRole> roles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(driveId);
        List<StudentEligibilityResult> eligResults = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(studentId, driveId);
        Optional<Application> appOpt = applicationRepository.findByStudentIdAndPlacementDriveId(studentId, driveId);
        List<ShortlistEntry> matchedShortlists = shortlistEntryRepository
                .findByPlacementDriveIdAndMatchStatus(driveId, ShortlistMatchStatus.MATCHED);

        Optional<ShortlistEntry> studentShortlist = matchedShortlists.stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getId().equals(studentId))
                .findFirst();

        Instant now = Instant.now();
        boolean deadlinePassed = drive.getApplicationDeadline() != null && now.isAfter(drive.getApplicationDeadline());

        ApplicationStatus appStatus = appOpt.map(Application.class::cast)
                .map(Application::getStatus)
                .orElse(ApplicationStatus.NOT_STARTED);

        EligibilityDecision overallDecision = computeOverallEligibility(eligResults);
        List<RoleEligibilityCardResponse> roleCards = mapRoleEligibilityCards(roles, eligResults);

        ShortlistMatchStatus shortlistStatus = studentShortlist.map(ShortlistEntry::getMatchStatus).orElse(null);
        String candidateName = studentShortlist.map(ShortlistEntry::getCandidateName).orElse(null);

        boolean isActionable = (overallDecision == EligibilityDecision.ELIGIBLE)
                && (appStatus != ApplicationStatus.APPLIED && appStatus != ApplicationStatus.SHORTLISTED && appStatus != ApplicationStatus.COMPLETED)
                && !deadlinePassed;

        String actionRequired = computeActionRequired(appStatus, overallDecision, shortlistStatus, deadlinePassed);

        return new StudentPlacementDetailResponse(
                drive.getId(),
                drive.getCompanyName(),
                drive.getTitle(),
                drive.getDescription(),
                drive.getApplicationDeadline(),
                drive.getReceivedAt(),
                null,
                null,
                overallDecision,
                appStatus,
                appOpt.map(Application::getId).orElse(null),
                appOpt.map(Application::getAppliedAt).orElse(null),
                shortlistStatus,
                candidateName,
                roleCards,
                actionRequired,
                isActionable,
                deadlinePassed
        );
    }

    // -------------------------------------------------------------------------
    // Applications, Notifications & Reminders
    // -------------------------------------------------------------------------

    public PageResponse<StudentApplicationResponse> getStudentApplications(Long studentId, Pageable pageable) {
        Page<Application> appsPage = applicationRepository.findByStudentId(studentId, pageable);
        List<StudentApplicationResponse> responses = appsPage.getContent().stream()
                .map(this::mapToStudentApplicationResponse)
                .toList();

        return new PageResponse<>(
                responses,
                appsPage.getNumber(),
                appsPage.getSize(),
                appsPage.getTotalElements(),
                appsPage.getTotalPages(),
                appsPage.isLast()
        );
    }

    public PageResponse<StudentNotificationResponse> getStudentNotifications(Long studentId,
                                                                             NotificationType type,
                                                                             Pageable pageable) {
        Page<Notification> page = (type != null)
                ? notificationRepository.findByStudentIdAndNotificationType(studentId, type, pageable)
                : notificationRepository.findByStudentId(studentId, pageable);

        List<StudentNotificationResponse> responses = page.getContent().stream()
                .map(n -> new StudentNotificationResponse(
                        n.getId(),
                        n.getIdempotencyKey(),
                        n.getNotificationType(),
                        n.getChannel(),
                        n.getStatus(),
                        n.getPlacementDrive() != null ? n.getPlacementDrive().getId() : null,
                        n.getPlacementDrive() != null ? n.getPlacementDrive().getCompanyName() : null,
                        n.getPlacementRole() != null ? n.getPlacementRole().getId() : null,
                        n.getPlacementRole() != null ? n.getPlacementRole().getRoleTitle() : null,
                        n.getMessagePayload(),
                        n.getSentAt(),
                        n.getCreatedAt()
                ))
                .toList();

        return new PageResponse<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public PageResponse<StudentReminderResponse> getStudentReminders(Long studentId, Pageable pageable) {
        Page<ReminderTask> page = reminderTaskRepository.findByStudentId(studentId, pageable);
        List<StudentReminderResponse> responses = page.getContent().stream()
                .map(r -> new StudentReminderResponse(
                        r.getId(),
                        r.getPlacementDrive().getId(),
                        r.getPlacementDrive().getCompanyName(),
                        r.getPlacementRole() != null ? r.getPlacementRole().getId() : null,
                        r.getPlacementRole() != null ? r.getPlacementRole().getRoleTitle() : null,
                        r.getScheduledFor(),
                        r.getIntervalMinutes(),
                        r.getRemindersSent(),
                        r.getMaxReminders(),
                        r.getCancelReason(),
                        r.getLastReminderAt(),
                        r.getStatus(),
                        r.getStatus() == ReminderStatus.PENDING,
                        r.getCompletedAt()
                ))
                .toList();

        return new PageResponse<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    // -------------------------------------------------------------------------
    // Helper Mappers
    // -------------------------------------------------------------------------

    private StudentPlacementDriveCardResponse mapDriveToCard(Long studentId, PlacementDrive drive) {
        Long driveId = drive.getId();
        List<PlacementRole> roles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(driveId);
        List<StudentEligibilityResult> eligResults = eligibilityResultRepository
                .findByStudentIdAndPlacementDriveId(studentId, driveId);
        Optional<Application> appOpt = applicationRepository.findByStudentIdAndPlacementDriveId(studentId, driveId);
        List<ShortlistEntry> matchedShortlists = shortlistEntryRepository
                .findByPlacementDriveIdAndMatchStatus(driveId, ShortlistMatchStatus.MATCHED);

        boolean isShortlisted = matchedShortlists.stream()
                .anyMatch(e -> e.getStudent() != null && e.getStudent().getId().equals(studentId));

        Instant now = Instant.now();
        boolean deadlinePassed = drive.getApplicationDeadline() != null && now.isAfter(drive.getApplicationDeadline());

        ApplicationStatus appStatus = appOpt.map(Application::getStatus).orElse(ApplicationStatus.NOT_STARTED);
        EligibilityDecision overallDecision = computeOverallEligibility(eligResults);
        List<RoleEligibilityCardResponse> roleCards = mapRoleEligibilityCards(roles, eligResults);

        ShortlistMatchStatus shortlistStatus = isShortlisted ? ShortlistMatchStatus.MATCHED : null;

        boolean isActionable = (overallDecision == EligibilityDecision.ELIGIBLE)
                && (appStatus != ApplicationStatus.APPLIED && appStatus != ApplicationStatus.SHORTLISTED && appStatus != ApplicationStatus.COMPLETED)
                && !deadlinePassed;

        String actionRequired = computeActionRequired(appStatus, overallDecision, shortlistStatus, deadlinePassed);

        return new StudentPlacementDriveCardResponse(
                drive.getId(),
                drive.getCompanyName(),
                drive.getTitle(),
                drive.getDescription(),
                drive.getApplicationDeadline(),
                drive.getReceivedAt(),
                overallDecision,
                appStatus,
                appOpt.map(Application::getId).orElse(null),
                shortlistStatus,
                roleCards,
                actionRequired,
                isActionable,
                deadlinePassed
        );
    }

    private StudentApplicationResponse mapToStudentApplicationResponse(Application app) {
        PlacementDrive drive = app.getPlacementDrive();
        PlacementRole role = app.getPlacementRole();

        List<ShortlistEntry> matched = shortlistEntryRepository
                .findByPlacementDriveIdAndMatchStatus(drive.getId(), ShortlistMatchStatus.MATCHED);
        boolean isShortlisted = matched.stream()
                .anyMatch(e -> e.getStudent() != null && e.getStudent().getId().equals(app.getStudent().getId()));

        return new StudentApplicationResponse(
                app.getId(),
                drive.getId(),
                drive.getCompanyName(),
                drive.getTitle(),
                role != null ? role.getId() : null,
                role != null ? role.getRoleTitle() : null,
                app.getStatus(),
                app.getAppliedAt(),
                isShortlisted ? ShortlistMatchStatus.MATCHED : null,
                drive.getApplicationDeadline(),
                app.getCreatedAt()
        );
    }

    private List<RoleEligibilityCardResponse> mapRoleEligibilityCards(List<PlacementRole> roles,
                                                                     List<StudentEligibilityResult> eligResults) {
        Map<Long, StudentEligibilityResult> resultMap = eligResults.stream()
                .filter(r -> r.getPlacementRole() != null)
                .collect(Collectors.toMap(r -> r.getPlacementRole().getId(), r -> r, (a, b) -> a));

        return roles.stream().map(role -> {
            StudentEligibilityResult result = resultMap.get(role.getId());
            EligibilityDecision decision = result != null ? result.getDecision() : EligibilityDecision.REVIEW_REQUIRED;
            List<String> explanations = extractHumanReadableReasons(result);

            Map<String, Object> criteria = role.getEligibilityCriteria();
            BigDecimal minCgpa = extractMinCgpa(criteria);
            List<String> branches = extractBranches(criteria);
            Integer arrears = extractArrears(criteria);
            String gender = extractGender(criteria);

            return new RoleEligibilityCardResponse(
                    role.getId(),
                    role.getRoleTitle(),
                    decision,
                    explanations,
                    minCgpa,
                    branches,
                    arrears,
                    gender
            );
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractMinCgpa(Map<String, Object> criteria) {
        if (criteria == null || !criteria.containsKey("min_cgpa")) return null;
        Object val = criteria.get("min_cgpa");
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractBranches(Map<String, Object> criteria) {
        if (criteria == null || !criteria.containsKey("eligible_branches")) return List.of();
        Object val = criteria.get("eligible_branches");
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private Integer extractArrears(Map<String, Object> criteria) {
        if (criteria == null || !criteria.containsKey("standing_arrears_allowed")) return null;
        Object val = criteria.get("standing_arrears_allowed");
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String extractGender(Map<String, Object> criteria) {
        if (criteria == null || !criteria.containsKey("gender_allowed")) return null;
        return Objects.toString(criteria.get("gender_allowed"), null);
    }

    private List<String> extractHumanReadableReasons(StudentEligibilityResult result) {
        if (result == null || result.getCriteriaResults() == null || result.getCriteriaResults().isEmpty()) {
            return List.of("Evaluation pending or student data incomplete.");
        }

        List<String> reasons = new ArrayList<>();
        for (Map<String, Object> criterion : result.getCriteriaResults()) {
            Object name = criterion.get("criterion");
            Object status = criterion.get("status");
            Object reason = criterion.get("reason");

            String statusIcon = "PASS".equals(status) ? "✓" : ("FAIL".equals(status) ? "✗" : "ℹ");
            if (reason != null && !reason.toString().isBlank()) {
                reasons.add(statusIcon + " " + reason);
            } else if (name != null) {
                reasons.add(statusIcon + " " + name + ": " + status);
            }
        }
        return reasons.isEmpty() ? List.of("Criteria evaluated.") : reasons;
    }

    private EligibilityDecision computeOverallEligibility(List<StudentEligibilityResult> results) {
        if (results == null || results.isEmpty()) {
            return EligibilityDecision.REVIEW_REQUIRED;
        }

        boolean hasEligible = results.stream().anyMatch(r -> r.getDecision() == EligibilityDecision.ELIGIBLE);
        if (hasEligible) {
            return EligibilityDecision.ELIGIBLE;
        }

        boolean allNotEligible = results.stream().allMatch(r -> r.getDecision() == EligibilityDecision.NOT_ELIGIBLE);
        if (allNotEligible) {
            return EligibilityDecision.NOT_ELIGIBLE;
        }

        return EligibilityDecision.REVIEW_REQUIRED;
    }

    private String computeActionRequired(ApplicationStatus appStatus,
                                        EligibilityDecision decision,
                                        ShortlistMatchStatus shortlistStatus,
                                        boolean deadlinePassed) {
        if (shortlistStatus == ShortlistMatchStatus.MATCHED || appStatus == ApplicationStatus.SHORTLISTED) {
            return "🎉 Shortlisted! Awaiting interview / test schedule.";
        }
        if (appStatus == ApplicationStatus.APPLIED) {
            return "Application submitted. Awaiting shortlist announcements.";
        }
        if (deadlinePassed) {
            return "Application deadline has closed.";
        }
        if (decision == EligibilityDecision.ELIGIBLE) {
            return "You are ELIGIBLE! Submit your application before deadline.";
        }
        if (decision == EligibilityDecision.NOT_ELIGIBLE) {
            return "Ineligible based on academic criteria.";
        }
        return "Profile update or manual review required.";
    }

    private boolean isProfileComplete(Student s) {
        return s.getName() != null && !s.getName().isBlank()
                && s.getRegistrationNumber() != null && !s.getRegistrationNumber().isBlank()
                && s.getBranch() != null && !s.getBranch().isBlank()
                && s.getBatch() != null
                && s.getCgpa() != null
                && s.getDegree() != null && !s.getDegree().isBlank();
    }
}

package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.StudentRequest;
import com.placementos.backend.domain.dto.StudentResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business service for student profile operations.
 *
 * Transaction strategy:
 *  - @Transactional on writes (create, update) to ensure atomicity.
 *  - Read-only queries are not annotated as they do not modify state.
 *
 * Business rules:
 *  - registration_number is globally unique; duplicates are rejected before hitting the DB.
 *  - neopat_id is optional but must also be globally unique when provided.
 *  - CGPA range [0.00, 10.00] is enforced by the database CHECK; the DTO's @DecimalMin/@DecimalMax
 *    is a first-line defence for better error messages.
 *  - No authentication is implemented at this stage.
 */
@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Creates a new student profile.
     * Rejects the request at the service layer if the registration number or NeoPAT ID
     * already belongs to another student, providing a clean error message before the
     * database unique constraint fires.
     */
    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw DuplicateResourceException.registrationNumber(request.getRegistrationNumber());
        }

        if (request.getNeopatId() != null && !request.getNeopatId().isBlank()
                && studentRepository.existsByNeopatId(request.getNeopatId())) {
            throw DuplicateResourceException.neopatId(request.getNeopatId());
        }

        Student student = new Student();
        applyRequest(student, request);
        return StudentResponse.from(studentRepository.save(student));
    }

    /**
     * Updates a student's profile fields.
     * Does not allow changing registration_number or neopat_id to a value that already
     * belongs to another student.
     */
    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.student(id));

        // Check uniqueness only if the value is actually changing.
        if (!student.getRegistrationNumber().equals(request.getRegistrationNumber())
                && studentRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw DuplicateResourceException.registrationNumber(request.getRegistrationNumber());
        }

        String newNeopatId = request.getNeopatId();
        if (newNeopatId != null && !newNeopatId.isBlank()) {
            if (!newNeopatId.equals(student.getNeopatId())
                    && studentRepository.existsByNeopatId(newNeopatId)) {
                throw DuplicateResourceException.neopatId(newNeopatId);
            }
        }

        applyRequest(student, request);
        return StudentResponse.from(studentRepository.save(student));
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    public StudentResponse findById(Long id) {
        return studentRepository.findById(id)
                .map(StudentResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.student(id));
    }

    public Optional<StudentResponse> findByRegistrationNumber(String registrationNumber) {
        return studentRepository.findByRegistrationNumber(registrationNumber)
                .map(StudentResponse::from);
    }

    public Optional<StudentResponse> findByNeopatId(String neopatId) {
        return studentRepository.findByNeopatId(neopatId)
                .map(StudentResponse::from);
    }

    public List<StudentResponse> findAll() {
        return studentRepository.findAll()
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Applies all fields from a request onto a Student entity. */
    private void applyRequest(Student student, StudentRequest request) {
        student.setRegistrationNumber(request.getRegistrationNumber());
        student.setNeopatId(
                (request.getNeopatId() != null && !request.getNeopatId().isBlank())
                ? request.getNeopatId()
                : null);
        student.setName(request.getName());
        student.setBranch(request.getBranch());
        student.setBatch(request.getBatch());
        student.setCgpa(request.getCgpa());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setDegree(request.getDegree());
        student.setSpecialization(request.getSpecialization());
        student.setStandingArrears(request.getStandingArrears());
        student.setGender(request.getGender());
    }
}

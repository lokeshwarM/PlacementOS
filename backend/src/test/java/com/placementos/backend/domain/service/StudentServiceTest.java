package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.StudentRequest;
import com.placementos.backend.domain.dto.StudentResponse;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link StudentService}.
 * Uses Mockito to avoid any database or Spring context dependency.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    StudentRepository studentRepository;

    @InjectMocks
    StudentService studentService;

    private StudentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new StudentRequest();
        validRequest.setRegistrationNumber("22BCE1234");
        validRequest.setNeopatId("NEO001");
        validRequest.setName("Lokeshwar M");
        validRequest.setBranch("CSE");
        validRequest.setBatch(2026);
        validRequest.setCgpa(new BigDecimal("8.50"));
        validRequest.setPhoneNumber("9876543210");
    }

    // -------------------------------------------------------------------------
    // createStudent — success
    // -------------------------------------------------------------------------
    @Test
    void createStudent_success() {
        when(studentRepository.existsByRegistrationNumber("22BCE1234")).thenReturn(false);
        when(studentRepository.existsByNeopatId("NEO001")).thenReturn(false);

        Student saved = buildSavedStudent(1L, "22BCE1234", "NEO001");
        when(studentRepository.save(any(Student.class))).thenReturn(saved);

        StudentResponse response = studentService.createStudent(validRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRegistrationNumber()).isEqualTo("22BCE1234");
        assertThat(response.getNeopatId()).isEqualTo("NEO001");
    }

    // -------------------------------------------------------------------------
    // createStudent — duplicate registration number
    // -------------------------------------------------------------------------
    @Test
    void createStudent_duplicateRegistrationNumber_throwsDuplicate() {
        when(studentRepository.existsByRegistrationNumber("22BCE1234")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("22BCE1234");

        verify(studentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createStudent — duplicate NeoPAT ID
    // -------------------------------------------------------------------------
    @Test
    void createStudent_duplicateNeopatId_throwsDuplicate() {
        when(studentRepository.existsByRegistrationNumber("22BCE1234")).thenReturn(false);
        when(studentRepository.existsByNeopatId("NEO001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("NEO001");

        verify(studentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createStudent — null NeoPAT ID is accepted
    // -------------------------------------------------------------------------
    @Test
    void createStudent_nullNeopatId_isAccepted() {
        validRequest.setNeopatId(null);
        when(studentRepository.existsByRegistrationNumber("22BCE1234")).thenReturn(false);
        Student saved = buildSavedStudent(2L, "22BCE1234", null);
        when(studentRepository.save(any(Student.class))).thenReturn(saved);

        StudentResponse response = studentService.createStudent(validRequest);

        assertThat(response.getNeopatId()).isNull();
        verify(studentRepository, never()).existsByNeopatId(any());
    }

    // -------------------------------------------------------------------------
    // findByRegistrationNumber — found
    // -------------------------------------------------------------------------
    @Test
    void findByRegistrationNumber_found() {
        Student student = buildSavedStudent(1L, "22BCE1234", "NEO001");
        when(studentRepository.findByRegistrationNumber("22BCE1234"))
                .thenReturn(Optional.of(student));

        Optional<StudentResponse> result = studentService.findByRegistrationNumber("22BCE1234");

        assertThat(result).isPresent();
        assertThat(result.get().getRegistrationNumber()).isEqualTo("22BCE1234");
    }

    // -------------------------------------------------------------------------
    // findById — not found
    // -------------------------------------------------------------------------
    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Student buildSavedStudent(Long id, String regNumber, String neopatId) {
        // Use reflection to set id since the setter is absent (id is generated).
        try {
            Student s = new Student();
            var idField = Student.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, id);
            s.setRegistrationNumber(regNumber);
            s.setNeopatId(neopatId);
            s.setName("Lokeshwar M");
            s.setBranch("CSE");
            s.setBatch(2026);
            s.setCgpa(new BigDecimal("8.50"));
            return s;
        } catch (Exception e) {
            throw new RuntimeException("Test helper failed to build student", e);
        }
    }
}

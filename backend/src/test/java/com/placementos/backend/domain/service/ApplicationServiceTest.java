package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Application;
import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.Student;
import com.placementos.backend.domain.enums.ApplicationStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.ApplicationRepository;
import com.placementos.backend.domain.repository.PlacementDriveRepository;
import com.placementos.backend.domain.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApplicationService} — duplicate protection.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock StudentRepository studentRepository;
    @Mock PlacementDriveRepository placementDriveRepository;

    @InjectMocks
    ApplicationService applicationService;

    @Test
    void createApplication_success() {
        when(applicationRepository.existsByStudentIdAndPlacementDriveId(1L, 10L)).thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(placementDriveRepository.findById(10L)).thenReturn(Optional.of(new PlacementDrive()));

        Application saved = new Application();
        saved.setStatus(ApplicationStatus.NOT_STARTED);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        Application result = applicationService.createApplication(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.NOT_STARTED);
    }

    @Test
    void createApplication_duplicate_throwsDuplicate() {
        when(applicationRepository.existsByStudentIdAndPlacementDriveId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(1L, 10L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("student 1")
                .hasMessageContaining("placement drive 10");

        verify(applicationRepository, never()).save(any());
    }
}

package com.placementos.backend.domain.service;

import com.placementos.backend.domain.dto.DocumentProcessingResultDto;
import com.placementos.backend.domain.dto.ShortlistCandidateDto;
import com.placementos.backend.domain.entity.*;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import com.placementos.backend.domain.enums.ShortlistMatchStatus;
import com.placementos.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShortlistMatchingServiceTest {

    @Mock
    private ShortlistEntryRepository shortlistEntryRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private PlacementDriveRepository placementDriveRepository;

    @Mock
    private PlacementRoleRepository placementRoleRepository;

    private ShortlistMatchingService service;

    private Student student1;
    private Student student2;
    private PlacementDrive drive;
    private PlacementRole role1;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        service = new ShortlistMatchingService(
                shortlistEntryRepository,
                studentRepository,
                attachmentRepository,
                placementDriveRepository,
                placementRoleRepository
        );

        student1 = new Student();
        student1.setId(1L);
        student1.setRegistrationNumber("21BCE1001");
        student1.setNeopatId("NP1001");
        student1.setName("Alice Smith");

        student2 = new Student();
        student2.setId(2L);
        student2.setRegistrationNumber("21BCE1002");
        student2.setNeopatId("NP1002");
        student2.setName("Bob Johnson");

        drive = new PlacementDrive();
        drive.setId(10L);
        drive.setCompanyName("Google");

        role1 = new PlacementRole("Software Engineer", 1);
        role1.setId(101L);
        role1.setPlacementDrive(drive);

        attachment = new Attachment();
        attachment.setFilename("Shortlist.xlsx");
        attachment.setPlacementDrive(drive);

        lenient().when(shortlistEntryRepository.save(any(ShortlistEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void matchByRegistrationNumber_success() {
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(studentRepository.findByRegistrationNumber("21BCE1001")).thenReturn(Optional.of(student1));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(10L)).thenReturn(List.of(role1));

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("SHORTLIST");
        resultDto.setConfidence(0.98);
        resultDto.setCandidates(List.of(
                new ShortlistCandidateDto("21BCE1001", null, "Alice Smith", "Software Engineer", "Row 2: 21BCE1001")
        ));

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertEquals(1, entries.size());
        ShortlistEntry entry = entries.get(0);
        assertEquals(ShortlistMatchStatus.MATCHED, entry.getMatchStatus());
        assertEquals("REGISTRATION_NUMBER", entry.getMatchMethod());
        assertEquals(student1, entry.getStudent());
        assertEquals(role1, entry.getPlacementRole());
        assertEquals("21BCE1001", entry.getRegistrationNumber());
    }

    @Test
    void matchByNeopatId_success() {
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(studentRepository.findByNeopatId("NP1002")).thenReturn(Optional.of(student2));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(10L)).thenReturn(List.of());

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("SHORTLIST");
        resultDto.setCandidates(List.of(
                new ShortlistCandidateDto(null, "NP1002", "Bob Johnson", null, "Row 3: NP1002")
        ));

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertEquals(1, entries.size());
        ShortlistEntry entry = entries.get(0);
        assertEquals(ShortlistMatchStatus.MATCHED, entry.getMatchStatus());
        assertEquals("NEOPAT_ID", entry.getMatchMethod());
        assertEquals(student2, entry.getStudent());
    }

    @Test
    void conflictingIdentifiers_resultsInAmbiguous() {
        // Reg No points to student1 (Alice), but NeoPAT points to student2 (Bob)
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(studentRepository.findByRegistrationNumber("21BCE1001")).thenReturn(Optional.of(student1));
        when(studentRepository.findByNeopatId("NP1002")).thenReturn(Optional.of(student2));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(10L)).thenReturn(List.of());

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("SHORTLIST");
        resultDto.setCandidates(List.of(
                new ShortlistCandidateDto("21BCE1001", "NP1002", "Alice Smith", null, "Conflicting row")
        ));

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertEquals(1, entries.size());
        ShortlistEntry entry = entries.get(0);
        assertEquals(ShortlistMatchStatus.AMBIGUOUS, entry.getMatchStatus());
        assertNull(entry.getStudent());
        assertTrue(entry.getMatchReason().contains("Conflicting identifiers"));
    }

    @Test
    void exactNormalizedNameMatch_singleStudent_matched() {
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(studentRepository.findAll()).thenReturn(List.of(student1, student2));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(10L)).thenReturn(List.of());

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("SHORTLIST");
        resultDto.setCandidates(List.of(
                new ShortlistCandidateDto(null, null, "Alice   Smith", null, "Name only row")
        ));

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertEquals(1, entries.size());
        ShortlistEntry entry = entries.get(0);
        assertEquals(ShortlistMatchStatus.MATCHED, entry.getMatchStatus());
        assertEquals("EXACT_NORMALIZED_NAME", entry.getMatchMethod());
        assertEquals(student1, entry.getStudent());
    }

    @Test
    void exactNormalizedNameMatch_multipleStudents_ambiguous() {
        Student duplicateAlice = new Student();
        duplicateAlice.setId(3L);
        duplicateAlice.setRegistrationNumber("21BCE1003");
        duplicateAlice.setName("ALICE SMITH");

        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(studentRepository.findAll()).thenReturn(List.of(student1, duplicateAlice));
        when(placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(10L)).thenReturn(List.of());

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("SHORTLIST");
        resultDto.setCandidates(List.of(
                new ShortlistCandidateDto(null, null, "Alice Smith", null, "Duplicate name row")
        ));

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertEquals(1, entries.size());
        ShortlistEntry entry = entries.get(0);
        assertEquals(ShortlistMatchStatus.AMBIGUOUS, entry.getMatchStatus());
        assertNull(entry.getStudent());
        assertTrue(entry.getMatchReason().contains("Found 2 students"));
    }

    @Test
    void ocrRequiredDocument_updatesAttachmentStatus() {
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setOcrRequired(true);

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertTrue(entries.isEmpty());
        assertEquals(AttachmentParsedStatus.OCR_REQUIRED, attachment.getParsedStatus());
        verify(attachmentRepository).save(attachment);
    }

    @Test
    void nonShortlistDocument_updatesAttachmentStatusToParsed() {
        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));

        DocumentProcessingResultDto resultDto = new DocumentProcessingResultDto();
        resultDto.setAttachmentId(50L);
        resultDto.setClassification("JOB_DESCRIPTION");

        List<ShortlistEntry> entries = service.processDocumentShortlist(resultDto);

        assertTrue(entries.isEmpty());
        assertEquals(AttachmentParsedStatus.PARSED, attachment.getParsedStatus());
        verify(attachmentRepository).save(attachment);
    }
}

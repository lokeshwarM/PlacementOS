package com.placementos.backend.domain.repository;

import com.placementos.backend.domain.entity.PlacementDrive;
import com.placementos.backend.domain.entity.PlacementRole;
import com.placementos.backend.domain.enums.DriveStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PlacementRoleRepositoryTest {

    @Autowired
    private PlacementDriveRepository placementDriveRepository;

    @Autowired
    private PlacementRoleRepository placementRoleRepository;

    @Test
    void saveAndFind_persistsDriveWithMultipleRolesAndJsonbEligibility() {
        PlacementDrive drive = new PlacementDrive();
        drive.setCompanyName("Google India");
        drive.setTitle("Google Campus Hiring 2027");
        drive.setSourceEmailId("msg-goog-test-1");
        drive.setStatus(DriveStatus.OPEN);
        drive.setEligibilityCriteria(Map.of(
                "minimumCgpa", 7.0,
                "allowedBatches", List.of("2027")
        ));

        PlacementRole role1 = new PlacementRole("Software Engineer", 1);
        role1.setRoleDescription("Core SWE position");
        role1.setEligibilityCriteria(Map.of("allowedBranches", List.of("CSE", "IT")));
        drive.addRole(role1);

        PlacementRole role2 = new PlacementRole("Data Scientist", 2);
        role2.setRoleDescription("AI/ML Research position");
        role2.setEligibilityCriteria(Map.of("minimumCgpa", 8.0, "allowedSpecializations", List.of("Data Science")));
        drive.addRole(role2);

        PlacementDrive savedDrive = placementDriveRepository.saveAndFlush(drive);
        assertNotNull(savedDrive.getId());

        List<PlacementRole> roles = placementRoleRepository.findByPlacementDriveIdOrderByRoleOrderAsc(savedDrive.getId());
        assertEquals(2, roles.size());
        assertEquals("Software Engineer", roles.get(0).getRoleTitle());
        assertEquals(1, roles.get(0).getRoleOrder());
        assertEquals("Data Scientist", roles.get(1).getRoleTitle());
        assertEquals(2, roles.get(1).getRoleOrder());

        // Verify JSONB retrieval
        assertNotNull(roles.get(1).getEligibilityCriteria());
        assertEquals(8.0, roles.get(1).getEligibilityCriteria().get("minimumCgpa"));

        // Find by drive and role title
        Optional<PlacementRole> foundRole = placementRoleRepository.findByPlacementDriveIdAndRoleTitle(savedDrive.getId(), "Software Engineer");
        assertTrue(foundRole.isPresent());
    }
}

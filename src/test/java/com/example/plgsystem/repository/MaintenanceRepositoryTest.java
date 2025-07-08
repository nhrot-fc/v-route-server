package com.example.plgsystem.repository;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class MaintenanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MaintenanceRepository maintenanceRepository;
    
    private Vehicle createTestVehicle(String id) {
        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        return entityManager.persist(vehicle);
    }

    @Test
    public void testSaveMaintenance() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        LocalDate assignedDate = LocalDate.of(2025, 1, 15);
        Maintenance maintenance = new Maintenance(vehicle, assignedDate);

        // When
        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);

        // Then
        assertNotNull(savedMaintenance);
        assertNotNull(savedMaintenance.getId()); // ID should be generated
        assertEquals("V001", savedMaintenance.getVehicle().getId());
        assertEquals(assignedDate, savedMaintenance.getAssignedDate());
        assertNull(savedMaintenance.getRealStart());
        assertNull(savedMaintenance.getRealEnd());
    }

    @Test
    public void testFindById() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        LocalDate assignedDate = LocalDate.of(2025, 1, 15);
        Maintenance maintenance = new Maintenance(vehicle, assignedDate);
        entityManager.persist(maintenance);
        entityManager.flush();

        UUID maintenanceId = maintenance.getId();

        // When
        Optional<Maintenance> found = maintenanceRepository.findById(maintenanceId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(maintenanceId, found.get().getId());
        assertEquals("V001", found.get().getVehicle().getId());
        assertEquals(assignedDate, found.get().getAssignedDate());
    }

    @Test
    public void testFindAll() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        
        LocalDate date1 = LocalDate.of(2025, 1, 15);
        LocalDate date2 = LocalDate.of(2025, 2, 15);

        Maintenance maintenance1 = new Maintenance(vehicle1, date1);
        Maintenance maintenance2 = new Maintenance(vehicle2, date2);

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.flush();

        // When
        List<Maintenance> maintenances = maintenanceRepository.findAll();

        // Then
        assertEquals(2, maintenances.size());
    }

    @Test
    public void testUpdateMaintenance() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        LocalDate assignedDate = LocalDate.of(2025, 1, 15);
        Maintenance maintenance = new Maintenance(vehicle, assignedDate);
        entityManager.persist(maintenance);
        entityManager.flush();

        UUID maintenanceId = maintenance.getId();

        // When
        Maintenance savedMaintenance = maintenanceRepository.findById(maintenanceId).get();
        LocalDateTime startTime = LocalDateTime.now();
        savedMaintenance.setRealStart(startTime);
        maintenanceRepository.save(savedMaintenance);

        // Then
        Maintenance updatedMaintenance = maintenanceRepository.findById(maintenanceId).get();
        assertEquals(startTime, updatedMaintenance.getRealStart());
    }

    @Test
    public void testDeleteMaintenance() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        LocalDate assignedDate = LocalDate.of(2025, 1, 15);
        Maintenance maintenance = new Maintenance(vehicle, assignedDate);
        entityManager.persist(maintenance);
        entityManager.flush();

        UUID maintenanceId = maintenance.getId();

        // When
        maintenanceRepository.deleteById(maintenanceId);

        // Then
        Optional<Maintenance> deleted = maintenanceRepository.findById(maintenanceId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindByVehicleId() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        
        LocalDate date1 = LocalDate.of(2025, 1, 15);
        LocalDate date2 = LocalDate.of(2025, 3, 15);
        LocalDate date3 = LocalDate.of(2025, 2, 15);

        Maintenance maintenance1 = new Maintenance(vehicle1, date1);
        Maintenance maintenance2 = new Maintenance(vehicle1, date2); // Same vehicle
        Maintenance maintenance3 = new Maintenance(vehicle2, date3); // Different vehicle

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.persist(maintenance3);
        entityManager.flush();

        // When
        List<Maintenance> vehicleMaintenances = maintenanceRepository.findByVehicleId("V001");

        // Then
        assertEquals(2, vehicleMaintenances.size());
        assertTrue(vehicleMaintenances.stream().allMatch(m -> m.getVehicle().getId().equals("V001")));
    }

    @Test
    public void testFindByAssignedDate() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");
        
        LocalDate date = LocalDate.of(2025, 1, 15);

        Maintenance maintenance1 = new Maintenance(vehicle1, date);
        Maintenance maintenance2 = new Maintenance(vehicle2, date); // Same date
        Maintenance maintenance3 = new Maintenance(vehicle3, date.plusDays(1)); // Different date

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.persist(maintenance3);
        entityManager.flush();

        // When
        List<Maintenance> dateMaintenances = maintenanceRepository.findByAssignedDate(date);

        // Then
        assertEquals(2, dateMaintenances.size());
        assertTrue(dateMaintenances.stream().allMatch(m -> m.getAssignedDate().equals(date)));
    }

    @Test
    public void testFindByVehicleIdAndAssignedDate() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        
        LocalDate date1 = LocalDate.of(2025, 1, 15);
        LocalDate date2 = LocalDate.of(2025, 2, 15);

        Maintenance maintenance1 = new Maintenance(vehicle1, date1);
        Maintenance maintenance2 = new Maintenance(vehicle1, date2); // Same vehicle, different date
        Maintenance maintenance3 = new Maintenance(vehicle2, date1); // Different vehicle, same date

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.persist(maintenance3);
        entityManager.flush();

        // When
        List<Maintenance> specificMaintenances = maintenanceRepository.findByVehicleIdAndAssignedDate("V001", date1);

        // Then
        assertEquals(1, specificMaintenances.size());
        assertEquals("V001", specificMaintenances.getFirst().getVehicle().getId());
        assertEquals(date1, specificMaintenances.getFirst().getAssignedDate());
    }

    @Test
    public void testFindByAssignedDateBetween() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");
        Vehicle vehicle4 = createTestVehicle("V004");
        
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 2, 1);
        LocalDate date3 = LocalDate.of(2025, 3, 1);
        LocalDate date4 = LocalDate.of(2025, 4, 1);

        Maintenance maintenance1 = new Maintenance(vehicle1, date1);
        Maintenance maintenance2 = new Maintenance(vehicle2, date2);
        Maintenance maintenance3 = new Maintenance(vehicle3, date3);
        Maintenance maintenance4 = new Maintenance(vehicle4, date4);

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.persist(maintenance3);
        entityManager.persist(maintenance4);
        entityManager.flush();

        // When - Find maintenances between January and March
        List<Maintenance> q1Maintenances = maintenanceRepository.findByAssignedDateBetween(
                date1, date3);

        // Then
        assertEquals(3, q1Maintenances.size());
        assertFalse(q1Maintenances.stream().anyMatch(m -> m.getVehicle().getId().equals("V004")));
    }

    @Test
    public void testFindActiveMaintenances() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");
        Vehicle vehicle4 = createTestVehicle("V004");
        
        Maintenance maintenance1 = new Maintenance(vehicle1, today);
        maintenance1.setRealStart(now.minusHours(2));
        maintenance1.setRealEnd(now.plusHours(2)); // Active (now is between start and end)

        Maintenance maintenance2 = new Maintenance(vehicle2, today);
        maintenance2.setRealStart(now.minusHours(4));
        maintenance2.setRealEnd(now.minusHours(2)); // Not active (already ended)

        Maintenance maintenance3 = new Maintenance(vehicle3, today);
        maintenance3.setRealStart(now.plusHours(2));
        maintenance3.setRealEnd(now.plusHours(4)); // Not active (not started yet)

        Maintenance maintenance4 = new Maintenance(vehicle4, today);
        // Not active (no start/end times)

        entityManager.persist(maintenance1);
        entityManager.persist(maintenance2);
        entityManager.persist(maintenance3);
        entityManager.persist(maintenance4);
        entityManager.flush();

        // When
        List<Maintenance> activeMaintenances = maintenanceRepository.findActiveMaintenances(now);

        // Then
        assertEquals(1, activeMaintenances.size());
        assertEquals("V001", activeMaintenances.getFirst().getVehicle().getId());
    }
} 
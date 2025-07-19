package com.example.plgsystem.repository;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Incident;
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
public class IncidentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IncidentRepository incidentRepository;

    private Vehicle createTestVehicle(String id) {
        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        return entityManager.persist(vehicle);
    }

    @Test
    public void testSaveIncident() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        Shift shift = Shift.T1;
        LocalDateTime occurrenceTime = shift.getStartTime().atDate(LocalDate.now());
        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);

        incident.setOccurrenceTime(occurrenceTime);

        // When
        Incident savedIncident = incidentRepository.save(incident);

        // Then
        assertNotNull(savedIncident);
        assertNotNull(savedIncident.getId()); // ID should be generated
        assertEquals("V001", savedIncident.getVehicle().getId());
        assertEquals(IncidentType.TI1, savedIncident.getType());
        assertEquals(Shift.T1, savedIncident.getShift());
        assertEquals(occurrenceTime, savedIncident.getOccurrenceTime());
        assertFalse(savedIncident.isResolved());
    }

    @Test
    public void testFindById() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        Shift shift = Shift.T1;
        LocalDateTime occurrenceTime = shift.getStartTime().atDate(LocalDate.now());
        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);
        incident.setOccurrenceTime(LocalDateTime.now());

        entityManager.persist(incident);
        entityManager.flush();

        UUID incidentId = incident.getId();

        // When
        Optional<Incident> found = incidentRepository.findById(incidentId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(incidentId, found.get().getId());
        assertEquals("V001", found.get().getVehicle().getId());
        assertEquals(IncidentType.TI1, found.get().getType());
    }

    @Test
    public void testFindAll() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");

        Shift shift1 = Shift.T1;
        LocalDateTime occurrenceTime1 = shift1.getStartTime().atDate(LocalDate.now());
        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);

        Shift shift2 = Shift.T2;
        LocalDateTime occurrenceTime2 = shift2.getStartTime().atDate(LocalDate.now());
        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime2);

        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.flush();

        // When
        List<Incident> incidents = incidentRepository.findAll();

        // Then
        assertEquals(2, incidents.size());
    }

    @Test
    public void testUpdateIncident() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        Shift shift = Shift.T1;
        LocalDateTime occurrenceTime = shift.getStartTime().atDate(LocalDate.now());
        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);
        incident.setOccurrenceTime(LocalDateTime.now());
        entityManager.persist(incident);
        entityManager.flush();

        UUID incidentId = incident.getId();

        // When
        Incident savedIncident = incidentRepository.findById(incidentId).get();
        savedIncident.setResolved(true);
        incidentRepository.save(savedIncident);

        // Then
        Incident updatedIncident = incidentRepository.findById(incidentId).get();
        assertTrue(updatedIncident.isResolved());
    }

    @Test
    public void testDeleteIncident() {
        // Given
        Vehicle vehicle = createTestVehicle("V001");
        Shift shift = Shift.T1;
        LocalDateTime occurrenceTime = shift.getStartTime().atDate(LocalDate.now());
        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);
        entityManager.persist(incident);
        entityManager.flush();

        UUID incidentId = incident.getId();

        // When
        incidentRepository.deleteById(incidentId);

        // Then
        Optional<Incident> deleted = incidentRepository.findById(incidentId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindByVehicleId() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");

        Shift shift1 = Shift.T1;
        LocalDateTime occurrenceTime1 = shift1.getStartTime().atDate(LocalDate.now());
        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);

        Shift shift2 = Shift.T2;
        LocalDateTime occurrenceTime2 = shift2.getStartTime().atDate(LocalDate.now());
        Incident incident2 = new Incident(vehicle1, IncidentType.TI2, occurrenceTime2); // Same vehicle

        Shift shift3 = Shift.T3;
        LocalDateTime occurrenceTime3 = shift3.getStartTime().atDate(LocalDate.now());
        Incident incident3 = new Incident(vehicle2, IncidentType.TI3, occurrenceTime3);

        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.persist(incident3);
        entityManager.flush();

        // When
        List<Incident> vehicleIncidents = incidentRepository.findByVehicleId("V001");

        // Then
        assertEquals(2, vehicleIncidents.size());
        assertTrue(vehicleIncidents.stream().allMatch(i -> i.getVehicle().getId().equals("V001")));
    }

    @Test
    public void testFindByResolved() {
        // Given
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");

        Shift shift1 = Shift.T1;
        LocalDateTime occurrenceTime1 = shift1.getStartTime().atDate(LocalDate.now());
        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);
        incident1.setResolved(true); // Resolved

        Shift shift2 = Shift.T2;
        LocalDateTime occurrenceTime2 = shift2.getStartTime().atDate(LocalDate.now());
        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime2); // Not resolved

        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.flush();

        // When
        List<Incident> resolvedIncidents = incidentRepository.findByResolved(true);
        List<Incident> unresolvedIncidents = incidentRepository.findByResolved(false);

        // Then
        assertEquals(1, resolvedIncidents.size());
        assertEquals(incident1.getId(), resolvedIncidents.getFirst().getId());

        assertEquals(1, unresolvedIncidents.size());
        assertEquals(incident2.getId(), unresolvedIncidents.getFirst().getId());
    }

    @Test
    public void testFindByOccurrenceTimeBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");

        Shift shift1 = Shift.T1;
        LocalDateTime occurrenceTime1 = shift1.getStartTime().atDate(LocalDate.now());
        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);
        incident1.setOccurrenceTime(yesterday);

        Shift shift2 = Shift.T2;
        LocalDateTime occurrenceTime2 = shift2.getStartTime().atDate(LocalDate.now());
        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime2);
        incident2.setOccurrenceTime(now);

        Shift shift3 = Shift.T3;
        LocalDateTime occurrenceTime3 = shift3.getStartTime().atDate(LocalDate.now());
        Incident incident3 = new Incident(vehicle3, IncidentType.TI3, occurrenceTime3);
        incident3.setOccurrenceTime(tomorrow);

        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.persist(incident3);
        entityManager.flush();

        // When - Find incidents between yesterday and tomorrow inclusive
        List<Incident> recentIncidents = incidentRepository.findByOccurrenceTimeBetween(
                yesterday.minusHours(1), tomorrow.plusHours(1)); // Add extra padding on both sides

        // Then
        assertEquals(3, recentIncidents.size());
        assertFalse(recentIncidents.stream().anyMatch(i -> i.getVehicle().getId().equals("V004")));
    }

    @Test
    public void testFindByVehicleIdAndOccurrenceTimeBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");

        Shift shift1 = Shift.T1;
        LocalDateTime occurrenceTime1 = shift1.getStartTime().atDate(LocalDate.now());
        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);
        incident1.setOccurrenceTime(yesterday);

        Shift shift2 = Shift.T2;
        LocalDateTime occurrenceTime2 = shift2.getStartTime().atDate(LocalDate.now());
        Incident incident2 = new Incident(vehicle1, IncidentType.TI2, occurrenceTime2);
        incident2.setOccurrenceTime(tomorrow);

        Shift shift3 = Shift.T3;
        LocalDateTime occurrenceTime3 = shift3.getStartTime().atDate(LocalDate.now());
        Incident incident3 = new Incident(vehicle2, IncidentType.TI3, occurrenceTime3);
        incident3.setOccurrenceTime(now);

        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.persist(incident3);
        entityManager.flush();

        // When - Find incidents for V001 between yesterday and tomorrow
        List<Incident> v1Incidents = incidentRepository.findByVehicleIdAndOccurrenceTimeBetween(
                "V001", yesterday.minusHours(1), tomorrow.plusHours(1));

        // Then
        assertEquals(2, v1Incidents.size());
        assertTrue(v1Incidents.stream().allMatch(i -> i.getVehicle().getId().equals("V001")));
    }
}
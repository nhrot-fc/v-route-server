package com.example.plgsystem.repository;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class IncidentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    public void testSaveIncident() {
        // Given
        Incident incident = new Incident("V001", IncidentType.TI1, Shift.T1);
        LocalDateTime occurrenceTime = LocalDateTime.now().minusHours(1);
        Position location = new Position(10, 20);
        
        incident.setOccurrenceTime(occurrenceTime);
        incident.setLocation(location);
        incident.setTransferableGlp(50.0);
        
        // When
        Incident savedIncident = incidentRepository.save(incident);
        
        // Then
        assertNotNull(savedIncident);
        assertNotNull(savedIncident.getId()); // ID should be generated
        assertEquals("V001", savedIncident.getVehicleId());
        assertEquals(IncidentType.TI1, savedIncident.getType());
        assertEquals(Shift.T1, savedIncident.getShift());
        assertEquals(occurrenceTime, savedIncident.getOccurrenceTime());
        assertEquals(location.getX(), savedIncident.getLocation().getX());
        assertEquals(location.getY(), savedIncident.getLocation().getY());
        assertEquals(50.0, savedIncident.getTransferableGlp());
        assertFalse(savedIncident.isResolved());
    }

    @Test
    public void testFindById() {
        // Given
        Incident incident = new Incident("V001", IncidentType.TI1, Shift.T1);
        incident.setOccurrenceTime(LocalDateTime.now());
        incident.setLocation(new Position(10, 20));
        
        entityManager.persist(incident);
        entityManager.flush();
        
        Long incidentId = incident.getId();
        
        // When
        Optional<Incident> found = incidentRepository.findById(incidentId);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(incidentId, found.get().getId());
        assertEquals("V001", found.get().getVehicleId());
        assertEquals(IncidentType.TI1, found.get().getType());
    }

    @Test
    public void testFindAll() {
        // Given
        Incident incident1 = new Incident("V001", IncidentType.TI1, Shift.T1);
        Incident incident2 = new Incident("V002", IncidentType.TI2, Shift.T2);
        
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
        Incident incident = new Incident("V001", IncidentType.TI1, Shift.T1);
        incident.setOccurrenceTime(LocalDateTime.now());
        entityManager.persist(incident);
        entityManager.flush();
        
        Long incidentId = incident.getId();
        
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
        Incident incident = new Incident("V001", IncidentType.TI1, Shift.T1);
        entityManager.persist(incident);
        entityManager.flush();
        
        Long incidentId = incident.getId();
        
        // When
        incidentRepository.deleteById(incidentId);
        
        // Then
        Optional<Incident> deleted = incidentRepository.findById(incidentId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindByVehicleId() {
        // Given
        Incident incident1 = new Incident("V001", IncidentType.TI1, Shift.T1);
        Incident incident2 = new Incident("V001", IncidentType.TI2, Shift.T2); // Same vehicle
        Incident incident3 = new Incident("V002", IncidentType.TI3, Shift.T3); // Different vehicle
        
        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.persist(incident3);
        entityManager.flush();
        
        // When
        List<Incident> vehicleIncidents = incidentRepository.findByVehicleId("V001");
        
        // Then
        assertEquals(2, vehicleIncidents.size());
        assertTrue(vehicleIncidents.stream().allMatch(i -> i.getVehicleId().equals("V001")));
    }

    @Test
    public void testFindByResolved() {
        // Given
        Incident incident1 = new Incident("V001", IncidentType.TI1, Shift.T1);
        incident1.setResolved(true); // Resolved
        
        Incident incident2 = new Incident("V002", IncidentType.TI2, Shift.T2); // Not resolved
        
        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.flush();
        
        // When
        List<Incident> resolvedIncidents = incidentRepository.findByResolved(true);
        List<Incident> unresolvedIncidents = incidentRepository.findByResolved(false);
        
        // Then
        assertEquals(1, resolvedIncidents.size());
        assertEquals(incident1.getId(), resolvedIncidents.get(0).getId());
        
        assertEquals(1, unresolvedIncidents.size());
        assertEquals(incident2.getId(), unresolvedIncidents.get(0).getId());
    }

    @Test
    public void testFindByOccurrenceTimeBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime nextWeek = now.plusDays(7);
        
        Incident incident1 = new Incident("V001", IncidentType.TI1, Shift.T1);
        incident1.setOccurrenceTime(yesterday);
        
        Incident incident2 = new Incident("V002", IncidentType.TI2, Shift.T2);
        incident2.setOccurrenceTime(now);
        
        Incident incident3 = new Incident("V003", IncidentType.TI3, Shift.T3);
        incident3.setOccurrenceTime(tomorrow);
        
        Incident incident4 = new Incident("V004", IncidentType.TI3, Shift.T3);
        incident4.setOccurrenceTime(nextWeek);
        
        entityManager.persist(incident1);
        entityManager.persist(incident2);
        entityManager.persist(incident3);
        entityManager.persist(incident4);
        entityManager.flush();
        
        // When - Find incidents between yesterday and tomorrow inclusive
        List<Incident> recentIncidents = incidentRepository.findByOccurrenceTimeBetween(
            yesterday.minusHours(1), tomorrow.plusHours(1));  // Add extra padding on both sides
        
        // Then
        assertEquals(3, recentIncidents.size());
        assertFalse(recentIncidents.stream().anyMatch(i -> i.getVehicleId().equals("V004")));
    }

    @Test
    public void testFindByVehicleIdAndOccurrenceTimeBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        
        Incident incident1 = new Incident("V001", IncidentType.TI1, Shift.T1);
        incident1.setOccurrenceTime(yesterday);
        
        Incident incident2 = new Incident("V001", IncidentType.TI2, Shift.T2);
        incident2.setOccurrenceTime(tomorrow);
        
        Incident incident3 = new Incident("V002", IncidentType.TI3, Shift.T3);
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
        assertTrue(v1Incidents.stream().allMatch(i -> i.getVehicleId().equals("V001")));
    }
} 
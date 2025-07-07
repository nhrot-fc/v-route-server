package com.example.plgsystem.service;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IncidentServiceTest {

    @InjectMocks
    private IncidentService incidentService;

    @Mock
    private IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_shouldReturnSavedIncident() {
        Shift shift = Shift.T1;
        LocalDateTime occurrenceTime = shift.getStartTime().atDate(LocalDate.now());

        Position position = new Position(10, 10);
        Vehicle vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);

        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        // Act
        Incident savedIncident = incidentService.save(incident);

        // Assert
        assertEquals(IncidentType.TI1, savedIncident.getType());
        assertEquals(occurrenceTime, savedIncident.getOccurrenceTime());
        assertEquals("V-001", savedIncident.getVehicle().getId());
        assertEquals(Shift.T1, savedIncident.getShift());
        verify(incidentRepository, times(1)).save(incident);
    }

    @Test
    void findById_shouldReturnIncidentWhenExists() {
        // Arrange
        UUID incidentId = UUID.randomUUID();
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position = new Position(10, 10);
        Vehicle vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        // Act
        Optional<Incident> result = incidentService.findById(incidentId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(IncidentType.TI1, result.get().getType());
        assertEquals("V-001", result.get().getVehicle().getId());
    }

    @Test
    void findAll_shouldReturnAllIncidents() {
        // Arrange
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position1 = new Position(10, 10);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime);

        Position position2 = new Position(20, 20);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime.plusHours(1));

        List<Incident> incidents = new ArrayList<>();
        incidents.add(incident1);
        incidents.add(incident2);

        when(incidentRepository.findAll()).thenReturn(incidents);

        // Act
        List<Incident> result = incidentService.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("V-001", result.get(0).getVehicle().getId());
        assertEquals("V-002", result.get(1).getVehicle().getId());
    }

    @Test
    void findAllPaged_shouldReturnPagedIncidents() {
        // Arrange
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position1 = new Position(10, 10);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime);

        Position position2 = new Position(20, 20);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime.plusHours(1));

        List<Incident> incidents = new ArrayList<>();
        incidents.add(incident1);
        incidents.add(incident2);

        Page<Incident> pagedIncidents = new PageImpl<>(incidents);
        Pageable pageable = PageRequest.of(0, 10);

        when(incidentRepository.findAll(pageable)).thenReturn(pagedIncidents);

        // Act
        Page<Incident> result = incidentService.findAllPaged(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
    }

    @Test
    void findByVehicleId_shouldReturnVehicleIncidents() {
        // Arrange
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position = new Position(10, 10);
        Vehicle vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        Incident incident1 = new Incident(vehicle, IncidentType.TI1, occurrenceTime);

        Incident incident2 = new Incident(vehicle, IncidentType.TI3, occurrenceTime);

        List<Incident> vehicleIncidents = new ArrayList<>();
        vehicleIncidents.add(incident1);
        vehicleIncidents.add(incident2);

        when(incidentRepository.findByVehicleId("V-001")).thenReturn(vehicleIncidents);

        // Act
        List<Incident> result = incidentService.findByVehicleId("V-001");

        // Assert
        assertEquals(2, result.size());
        assertEquals("V-001", result.get(0).getVehicle().getId());
        assertEquals("V-001", result.get(1).getVehicle().getId());
    }

    @Test
    void findByType_shouldReturnIncidentsOfType() {
        // Arrange
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position1 = new Position(10, 10);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime);

        Position position2 = new Position(20, 20);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        Incident incident2 = new Incident(vehicle2, IncidentType.TI1, occurrenceTime.plusHours(1));

        List<Incident> mechanicalIncidents = new ArrayList<>();
        mechanicalIncidents.add(incident1);
        mechanicalIncidents.add(incident2);

        when(incidentRepository.findByType(IncidentType.TI1)).thenReturn(mechanicalIncidents);

        // Act
        List<Incident> result = incidentService.findByType(IncidentType.TI1);

        // Assert
        assertEquals(2, result.size());
        assertEquals(IncidentType.TI1, result.get(0).getType());
        assertEquals(IncidentType.TI1, result.get(1).getType());
    }

    @Test
    void findByShift_shouldReturnIncidentsInShift() {
        // Arrange
        LocalDateTime morningTime = LocalDateTime.now().withHour(5); // 5 AM - T1

        Position position1 = new Position(10, 10);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, morningTime);

        Position position2 = new Position(20, 20);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, morningTime.plusHours(1));

        List<Incident> morningIncidents = new ArrayList<>();
        morningIncidents.add(incident1);
        morningIncidents.add(incident2);

        when(incidentRepository.findByShift(Shift.T1)).thenReturn(morningIncidents);

        // Act
        List<Incident> result = incidentService.findByShift(Shift.T1);

        // Assert
        assertEquals(2, result.size());
        assertEquals(Shift.T1, result.get(0).getShift());
        assertEquals(Shift.T1, result.get(1).getShift());
    }

    @Test
    void findByResolved_shouldReturnResolvedIncidents() {
        // Arrange
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position1 = new Position(10, 10);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Incident incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime);
        incident1.setResolved(true);

        Position position2 = new Position(20, 20);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        Incident incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime.plusHours(1));
        incident2.setResolved(true);

        List<Incident> resolvedIncidents = new ArrayList<>();
        resolvedIncidents.add(incident1);
        resolvedIncidents.add(incident2);

        when(incidentRepository.findByResolved(true)).thenReturn(resolvedIncidents);

        // Act
        List<Incident> result = incidentService.findByResolved(true);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).isResolved());
        assertTrue(result.get(1).isResolved());
    }

    @Test
    void resolveIncident_shouldMarkIncidentAsResolved() {
        // Arrange
        UUID incidentId = UUID.randomUUID();
        LocalDateTime occurrenceTime = LocalDateTime.now();

        Position position = new Position(10, 10);
        Vehicle vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        Incident incident = new Incident(vehicle, IncidentType.TI1, occurrenceTime);
        incident.setResolved(false);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> {
            Incident savedIncident = (Incident) i.getArguments()[0];
            return savedIncident;
        });

        // Act
        Optional<Incident> result = incidentService.resolveIncident(incidentId);

        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.get().isResolved());
        verify(incidentRepository, times(1)).save(incident);
    }
}
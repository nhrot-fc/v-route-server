package com.example.plgsystem.model;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentTest {

    private Incident incident;
    private Vehicle vehicle;
    private final IncidentType incidentType = IncidentType.TI1;
    private final LocalDateTime occurrenceTime = LocalDateTime.of(2025, 1, 15, 10, 0); // 10:00 AM - T2

    @BeforeEach
    public void setUp() {
        Position position = new Position(10, 20);
        vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        incident = new Incident(vehicle, incidentType, occurrenceTime);
    }

    @Test
    public void testIncidentCreation() {
        assertNotNull(incident);
        assertEquals(vehicle, incident.getVehicle());
        assertEquals(incidentType, incident.getType());
        assertEquals(occurrenceTime, incident.getOccurrenceTime());
        // Verificar que el turno se calcula correctamente en base a la hora (10 AM = T2)
        assertEquals(Shift.T2, incident.getShift());
        assertFalse(incident.isResolved());
        assertEquals(0, incident.getTransferableGlp());
    }

    @Test
    public void testGetImmobilizationEndTime() {
        // Calcular el tiempo esperado basado en el tipo de incidente
        LocalDateTime expectedEndTime = occurrenceTime.plusHours(incidentType.getImmobilizationHours());
        assertEquals(expectedEndTime, incident.getImmobilizationEndTime());
    }

    @Test
    public void testGetAvailabilityTime() {
        // Calcular el tiempo esperado basado en el tipo de incidente
        LocalDateTime expectedEndTime = occurrenceTime
                .plusHours(incidentType.getImmobilizationHours())
                .plusHours(incidentType.getRepairHours());

        assertEquals(expectedEndTime, incident.getAvailabilityTime());
    }

    @Test
    public void testIsReturnToDepotRequired() {
        // For TI1, which has repair hours = 0
        assertFalse(incident.isReturnToDepotRequired(), "TI1 should not require return to depot");
        
        // Create an incident with TI2 which has repair hours > 0
        IncidentType typeWithRepair = IncidentType.TI2;
        Incident incidentWithRepair = new Incident(vehicle, typeWithRepair, occurrenceTime);
        assertTrue(incidentWithRepair.isReturnToDepotRequired(), "TI2 should require return to depot");
        
        // Create an incident with TI3 which has repair hours > 0
        IncidentType typeWithMoreRepair = IncidentType.TI3;
        Incident incidentWithMoreRepair = new Incident(vehicle, typeWithMoreRepair, occurrenceTime);
        assertTrue(incidentWithMoreRepair.isReturnToDepotRequired(), "TI3 should require return to depot");
    }

    @Test
    public void testSetAndGetResolved() {
        assertFalse(incident.isResolved());

        incident.setResolved(true);
        assertTrue(incident.isResolved());
    }

    @Test
    public void testSetAndGetTransferableGlp() {
        assertEquals(0, incident.getTransferableGlp());

        double transferableGlp = 10.5;
        incident.setTransferableGlp(transferableGlp);
        assertEquals(transferableGlp, incident.getTransferableGlp());
    }

    @Test
    public void testDifferentIncidentTypes() {
        // Crear incidentes con diferentes tipos
        Incident incident1 = new Incident(vehicle, IncidentType.TI1, occurrenceTime);
        Incident incident3 = new Incident(vehicle, IncidentType.TI3, occurrenceTime);

        // Verificar tiempos de inmovilización diferentes
        assertTrue(incident1.getImmobilizationEndTime().isBefore(incident3.getImmobilizationEndTime()));

        // Verificar tiempos de disponibilidad diferentes
        assertTrue(incident1.getAvailabilityTime().isBefore(incident3.getAvailabilityTime()));
    }

    @Test
    public void testDifferentShifts() {
        // Crear incidentes en diferentes turnos
        LocalDateTime morningTime = LocalDateTime.of(2025, 1, 15, 5, 0); // 5 AM - T1
        LocalDateTime afternoonTime = LocalDateTime.of(2025, 1, 15, 14, 0); // 2 PM - T2
        LocalDateTime nightTime = LocalDateTime.of(2025, 1, 15, 22, 0); // 10 PM - T3

        Incident morningIncident = new Incident(vehicle, incidentType, morningTime);
        Incident afternoonIncident = new Incident(vehicle, incidentType, afternoonTime);
        Incident nightIncident = new Incident(vehicle, incidentType, nightTime);

        assertEquals(Shift.T1, morningIncident.getShift());
        assertEquals(Shift.T2, afternoonIncident.getShift());
        assertEquals(Shift.T3, nightIncident.getShift());
    }
}
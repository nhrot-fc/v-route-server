package com.example.plgsystem.model;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentTest {

    private Incident incident;
    private final String vehicleId = "V001";
    private final IncidentType incidentType = IncidentType.TYPE_1;
    private final Shift shift = Shift.T1;
    private final Position location = new Position(10, 20);
    private final LocalDateTime occurrenceTime = LocalDateTime.now().minusHours(1);

    @BeforeEach
    public void setUp() {
        incident = new Incident(vehicleId, incidentType, shift);
    }

    @Test
    public void testIncidentCreation() {
        assertNotNull(incident);
        assertEquals(vehicleId, incident.getVehicleId());
        assertEquals(incidentType, incident.getType());
        assertEquals(shift, incident.getShift());
        assertFalse(incident.isResolved());
        assertEquals(0, incident.getTransferableGlp());
        assertNull(incident.getOccurrenceTime());
        assertNull(incident.getLocation());
    }

    @Test
    public void testSetOccurrenceTimeAndLocation() {
        incident.setOccurrenceTime(occurrenceTime);
        incident.setLocation(location);
        
        assertEquals(occurrenceTime, incident.getOccurrenceTime());
        assertEquals(location, incident.getLocation());
    }

    @Test
    public void testSetResolved() {
        assertFalse(incident.isResolved());
        
        incident.setResolved(true);
        assertTrue(incident.isResolved());
        
        incident.setResolved(false);
        assertFalse(incident.isResolved());
    }

    @Test
    public void testSetTransferableGlp() {
        assertEquals(0, incident.getTransferableGlp());
        
        double transferableGlp = 50.5;
        incident.setTransferableGlp(transferableGlp);
        
        assertEquals(transferableGlp, incident.getTransferableGlp());
    }

    @Test
    public void testCalculateAvailabilityTime() {
        // No occurrence time set
        assertNull(incident.calculateAvailabilityTime());
        
        // Set occurrence time
        incident.setOccurrenceTime(occurrenceTime);
        
        // Calculate expected availability time based on incident type
        LocalDateTime expectedAvailability = occurrenceTime
                .plusHours(incidentType.getImmobilizationHours())
                .plusHours(incidentType.getRepairHours());
                
        assertEquals(expectedAvailability, incident.calculateAvailabilityTime());
        
        // Try with a different incident type
        Incident criticalIncident = new Incident(vehicleId, IncidentType.TYPE_3, shift);
        criticalIncident.setOccurrenceTime(occurrenceTime);
        
        LocalDateTime criticalExpectedAvailability = occurrenceTime
                .plusHours(IncidentType.TYPE_3.getImmobilizationHours())
                .plusHours(IncidentType.TYPE_3.getRepairHours());
                
        assertEquals(criticalExpectedAvailability, criticalIncident.calculateAvailabilityTime());
    }

    @Test
    public void testRequiresReturnToDepot() {
        // TYPE_1 should not require return to depot
        assertFalse(incident.requiresReturnToDepot());
        
        // TYPE_3 should require return to depot
        Incident criticalIncident = new Incident(vehicleId, IncidentType.TYPE_3, shift);
        assertTrue(criticalIncident.requiresReturnToDepot());
    }

    @Test
    public void testToString() {
        // Initial toString without occurrence details
        String initialString = incident.toString();
        assertTrue(initialString.contains(vehicleId));
        assertTrue(initialString.contains(incidentType.toString()));
        assertTrue(initialString.contains("Not yet occurred"));
        
        // Set occurrence details
        incident.setOccurrenceTime(occurrenceTime);
        incident.setLocation(location);
        incident.setTransferableGlp(75.5);
        
        String detailedString = incident.toString();
        assertTrue(detailedString.contains(vehicleId));
        assertTrue(detailedString.contains(occurrenceTime.toString()));
        assertTrue(detailedString.contains(location.toString()));
        assertTrue(detailedString.contains("75.5"));
        assertTrue(detailedString.contains("Active"));
        
        // Resolve the incident
        incident.setResolved(true);
        
        String resolvedString = incident.toString();
        assertTrue(resolvedString.contains("Resolved"));
    }
} 
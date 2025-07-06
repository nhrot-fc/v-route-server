package com.example.plgsystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class MaintenanceTest {

    private Maintenance maintenance;
    private final String vehicleId = "V001";
    private final LocalDate assignedDate = LocalDate.now();

    @BeforeEach
    public void setUp() {
        maintenance = new Maintenance(vehicleId, assignedDate);
    }

    @Test
    public void testMaintenanceCreation() {
        assertNotNull(maintenance);
        assertEquals(vehicleId, maintenance.getVehicleId());
        assertEquals(assignedDate, maintenance.getAssignedDate());
        assertNull(maintenance.getRealStart());
        assertNull(maintenance.getRealEnd());
    }

    @Test
    public void testSetRealStartAndEnd() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now();
        
        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);
        
        assertEquals(startTime, maintenance.getRealStart());
        assertEquals(endTime, maintenance.getRealEnd());
        assertEquals(startTime.toLocalDate(), maintenance.getDate());
    }

    @Test
    public void testGetDate() {
        assertNull(maintenance.getDate());
        
        LocalDateTime startTime = LocalDateTime.now();
        maintenance.setRealStart(startTime);
        
        assertEquals(startTime.toLocalDate(), maintenance.getDate());
    }

    @Test
    public void testGetDurationHours() {
        // Initially should be 0 as realStart and realEnd are null
        assertEquals(0, maintenance.getDurationHours());
        
        // Set real start and end times 3 hours apart
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime endTime = startTime.plusHours(3);
        
        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);
        
        assertEquals(3, maintenance.getDurationHours());
        
        // If only start time is set, duration should still be 0
        maintenance = new Maintenance(vehicleId, assignedDate);
        maintenance.setRealStart(startTime);
        assertEquals(0, maintenance.getDurationHours());
    }

    @Test
    public void testIsActiveAt() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Initially should not be active as start/end are null
        assertFalse(maintenance.isActiveAt(currentTime));
        
        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);
        
        // Current time is between start and end time
        assertTrue(maintenance.isActiveAt(currentTime));
        
        // Time before maintenance started
        assertFalse(maintenance.isActiveAt(startTime.minusMinutes(1)));
        
        // Time after maintenance ended
        assertFalse(maintenance.isActiveAt(endTime.plusMinutes(1)));
        
        // Exact start and end times should be included in active period
        assertTrue(maintenance.isActiveAt(startTime));
        assertTrue(maintenance.isActiveAt(endTime));
    }

    @Test
    public void testCreateNextTask() {
        // Cannot create next task before current is completed
        assertNull(maintenance.createNextTask());
        
        // Complete the current task
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime endTime = startTime.plusHours(3);
        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);
        
        // Now create next task
        Maintenance nextTask = maintenance.createNextTask();
        
        assertNotNull(nextTask);
        assertEquals(vehicleId, nextTask.getVehicleId());
        assertEquals(endTime.toLocalDate().plusMonths(2), nextTask.getAssignedDate());
        assertNull(nextTask.getRealStart());
        assertNull(nextTask.getRealEnd());
    }

    @Test
    public void testFromString() {
        // Valid format
        String validRecord = "20250101:V001";
        Maintenance fromString = Maintenance.fromString(validRecord);
        
        assertNotNull(fromString);
        assertEquals("V001", fromString.getVehicleId());
        assertEquals(LocalDate.of(2025, 1, 1), fromString.getAssignedDate());
        
        // Invalid format (missing colon)
        String invalidRecord = "20250101V001";
        assertNull(Maintenance.fromString(invalidRecord));
        
        // Invalid format (invalid date)
        String invalidDateRecord = "2025-01-01:V001";
        assertNull(Maintenance.fromString(invalidDateRecord));
    }

    @Test
    public void testToString() {
        String maintenanceString = maintenance.toString();
        
        assertTrue(maintenanceString.contains(vehicleId));
        assertTrue(maintenanceString.contains(assignedDate.toString()));
        assertTrue(maintenanceString.contains("Scheduled"));
        
        // With start time
        maintenance.setRealStart(LocalDateTime.now());
        maintenanceString = maintenance.toString();
        assertTrue(maintenanceString.contains("In Progress"));
        
        // With both start and end times
        maintenance.setRealEnd(LocalDateTime.now().plusHours(2));
        maintenanceString = maintenance.toString();
        assertTrue(maintenanceString.contains("Completed"));
    }

    @Test
    public void testToRecordString() {
        // Without real start time
        String recordString = maintenance.toRecordString();
        String expectedFormat = assignedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ":" + vehicleId;
        assertEquals(expectedFormat, recordString);
        
        // With real start time
        LocalDateTime startTime = LocalDateTime.of(2025, 3, 15, 10, 0);
        maintenance.setRealStart(startTime);
        recordString = maintenance.toRecordString();
        expectedFormat = startTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ":" + vehicleId;
        assertEquals(expectedFormat, recordString);
    }
} 
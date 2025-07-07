package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MaintenanceTest {

    private Maintenance maintenance;
    private Vehicle vehicle;
    private final LocalDate assignedDate = LocalDate.now();

    @BeforeEach
    public void setUp() {
        Position position = new Position(10, 20);
        vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        maintenance = new Maintenance(vehicle, assignedDate);
    }

    @Test
    public void testMaintenanceCreation() {
        assertNotNull(maintenance);
        assertEquals(vehicle, maintenance.getVehicle());
        assertEquals(assignedDate, maintenance.getAssignedDate());
        assertNull(maintenance.getRealStart());
        assertNull(maintenance.getRealEnd());
    }

    @Test
    public void testSetAndGetRealStartAndEnd() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now();

        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);

        assertEquals(startTime, maintenance.getRealStart());
        assertEquals(endTime, maintenance.getRealEnd());
    }

    @Test
    public void testCreateNextTask() {
        // No se puede crear una siguiente tarea si no hay fecha de finalización
        assertNull(maintenance.createNextTask());

        // Completar la tarea actual
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime endTime = startTime.plusHours(24);
        maintenance.setRealStart(startTime);
        maintenance.setRealEnd(endTime);

        // Crear siguiente tarea
        Maintenance nextTask = maintenance.createNextTask();

        assertNotNull(nextTask);
        assertEquals(vehicle, nextTask.getVehicle());
        assertEquals(endTime.toLocalDate().plusMonths(2), nextTask.getAssignedDate());
        assertNull(nextTask.getRealStart());
        assertNull(nextTask.getRealEnd());
    }

    @Test
    public void testMaintenanceDates() {
        // Crear mantenimiento con fecha asignada
        LocalDate testDate = LocalDate.of(2025, 5, 15);
        Maintenance dateMaintenance = new Maintenance(vehicle, testDate);

        assertEquals(testDate, dateMaintenance.getAssignedDate());

        // Establecer fechas reales
        LocalDateTime startDateTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endDateTime = startDateTime.plusHours(24);

        dateMaintenance.setRealStart(startDateTime);
        dateMaintenance.setRealEnd(endDateTime);

        assertEquals(startDateTime, dateMaintenance.getRealStart());
        assertEquals(endDateTime, dateMaintenance.getRealEnd());
    }

    @Test
    public void testCreateNextTaskTimeline() {
        // Configurar una secuencia de mantenimientos
        LocalDate firstDate = LocalDate.of(2025, 1, 15);
        Maintenance firstMaintenance = new Maintenance(vehicle, firstDate);

        // Completar el primer mantenimiento
        LocalDateTime firstStart = LocalDateTime.of(2025, 1, 15, 9, 0);
        LocalDateTime firstEnd = firstStart.plusHours(24);
        firstMaintenance.setRealStart(firstStart);
        firstMaintenance.setRealEnd(firstEnd);

        // Crear segundo mantenimiento
        Maintenance secondMaintenance = firstMaintenance.createNextTask();
        assertNotNull(secondMaintenance);

        // Verificar que la fecha asignada es 2 meses después
        LocalDate expectedSecondDate = firstEnd.toLocalDate().plusMonths(2);
        assertEquals(expectedSecondDate, secondMaintenance.getAssignedDate());

        // Completar el segundo mantenimiento
        LocalDateTime secondStart = LocalDateTime.of(expectedSecondDate.getYear(),
                expectedSecondDate.getMonth(),
                expectedSecondDate.getDayOfMonth(), 9, 0);
        LocalDateTime secondEnd = secondStart.plusHours(24);
        secondMaintenance.setRealStart(secondStart);
        secondMaintenance.setRealEnd(secondEnd);

        // Crear tercer mantenimiento
        Maintenance thirdMaintenance = secondMaintenance.createNextTask();
        assertNotNull(thirdMaintenance);

        // Verificar que la fecha asignada es 2 meses después del segundo
        LocalDate expectedThirdDate = secondEnd.toLocalDate().plusMonths(2);
        assertEquals(expectedThirdDate, thirdMaintenance.getAssignedDate());
    }
}
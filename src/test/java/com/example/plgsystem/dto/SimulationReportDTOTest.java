package com.example.plgsystem.dto;

import com.example.plgsystem.orchest.SimulationStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationReportDTOTest {

    @Test
    public void testFromSimulationStats() {
        // Create a SimulationStats instance with sample data
        SimulationStats stats = new SimulationStats();
        
        // Set basic info
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        stats.startSimulation(startTime);
        stats.endSimulation(endTime);
        stats.setRealExecutionTimeMillis(5000);
        
        // Set order metrics
        for (int i = 0; i < 10; i++) {
            stats.recordNewOrder();
        }
        // 8 delivered (2 late)
        for (int i = 0; i < 6; i++) {
            stats.recordDeliveredOrder(false); // On-time deliveries
        }
        for (int i = 0; i < 2; i++) {
            stats.recordDeliveredOrder(true);  // Late deliveries
        }
        
        // Set vehicle metrics
        stats.recordVehicleOperation("V001", 100, 10, Duration.ofHours(5));
        stats.recordVehicleOperation("V002", 150, 15, Duration.ofHours(7));
        stats.recordVehicleBreakdown("V001");
        stats.recordVehicleBreakdown("V002");
        stats.recordMaintenanceEvent();
        
        // Set blockage metrics
        stats.recordBlockage(Duration.ofHours(2));
        stats.recordBlockage(Duration.ofHours(3));
        
        // Set algorithm metrics
        stats.recordReplan(300); // 300ms planning time
        stats.recordReplan(500); // 500ms planning time
        
        // Convert to DTO
        SimulationReportDTO report = SimulationReportDTO.fromSimulationStats(
            "sim-123",
            "weekly",
            "Test Weekly Simulation",
            stats
        );
        
        // Verify basic info
        assertEquals("sim-123", report.getSimulationId());
        assertEquals("weekly", report.getSimulationType());
        assertEquals("Test Weekly Simulation", report.getSimulationName());
        assertEquals(startTime, report.getStartDateTime());
        assertEquals(endTime, report.getEndDateTime());
        assertEquals(Duration.between(startTime, endTime), report.getTotalDuration());
        assertEquals(5000, report.getRealExecutionTimeMillis());
        
        // Verify order metrics
        assertEquals(10, report.getTotalOrders());
        assertEquals(8, report.getDeliveredOrders());
        assertEquals(2, report.getPendingOrders());
        assertEquals(2, report.getLateDeliveries());
        assertEquals(0.75, report.getOnTimeDeliveryRate()); // 6 on-time out of 8 delivered
        
        // Verify vehicle metrics
        assertEquals(250, report.getTotalDistanceTraveled());
        assertEquals(25, report.getTotalFuelConsumed());
        assertEquals(10.0, report.getAverageFuelEfficiency());
        assertEquals(2, report.getTotalVehicleBreakdowns());
        assertEquals(1, report.getTotalMaintenanceEvents());
        
        // Verify blockage metrics
        assertEquals(2, report.getTotalBlockages());
        assertEquals(Duration.ofHours(5), report.getTotalBlockageDuration());
        
        // Verify algorithm metrics
        assertEquals(2, report.getTotalReplans());
        assertEquals(400.0, report.getAveragePlanningTimeMillis());
    }
    
    @Test
    public void testEmptyStats() {
        // Create empty stats
        SimulationStats stats = new SimulationStats();
        
        // Convert to DTO
        SimulationReportDTO report = SimulationReportDTO.fromSimulationStats(
            "sim-empty",
            "weekly",
            "Empty Simulation",
            stats
        );
        
        // Verify default values
        assertEquals("sim-empty", report.getSimulationId());
        assertEquals(0, report.getTotalOrders());
        assertEquals(0, report.getDeliveredOrders());
        assertEquals(0, report.getLateDeliveries());
        assertEquals(0.0, report.getOnTimeDeliveryRate());
        assertEquals(0.0, report.getTotalDistanceTraveled());
        assertEquals(0.0, report.getTotalFuelConsumed());
        assertEquals(0.0, report.getAverageFuelEfficiency());
        assertEquals(0, report.getTotalVehicleBreakdowns());
        assertEquals(0, report.getTotalReplans());
    }
} 
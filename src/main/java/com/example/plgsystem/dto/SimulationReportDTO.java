package com.example.plgsystem.dto;

import com.example.plgsystem.orchest.SimulationStats;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for simulation reports generated at the end of simulations.
 * Contains summary statistics of the entire simulation run.
 */
@Data
public class SimulationReportDTO {
    private String simulationId;
    private String simulationType; // "daily", "weekly", or "collapse"
    private String simulationName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Duration totalDuration;
    private long realExecutionTimeMillis;
    
    // Order statistics
    private int totalOrders;
    private int deliveredOrders;
    private int pendingOrders;
    private int lateDeliveries;
    private double onTimeDeliveryRate;
    
    // Vehicle statistics
    private double totalDistanceTraveled;
    private double totalFuelConsumed;
    private double averageFuelEfficiency;
    private int totalVehicleBreakdowns;
    private int totalMaintenanceEvents;
    
    // Blockage statistics
    private int totalBlockages;
    private Duration totalBlockageDuration;
    
    // Algorithm statistics
    private int totalReplans;
    private double averagePlanningTimeMillis;
    
    // Vehicle utilization statistics
    private int totalVehicles;
    private double averageVehicleUtilization;
    
    /**
     * Creates a SimulationReportDTO from SimulationStats
     */
    public static SimulationReportDTO fromSimulationStats(
            String simulationId, 
            String simulationType,
            String simulationName,
            SimulationStats stats) {
        
        SimulationReportDTO report = new SimulationReportDTO();
        report.setSimulationId(simulationId);
        report.setSimulationType(simulationType);
        report.setSimulationName(simulationName);
        report.setStartDateTime(stats.getSimulationStartTime());
        report.setEndDateTime(stats.getSimulationEndTime());
        report.setTotalDuration(stats.getSimulationDuration());
        report.setRealExecutionTimeMillis(stats.getRealExecutionTimeMillis());
        
        // Order statistics
        report.setTotalOrders(stats.getTotalOrders());
        report.setDeliveredOrders(stats.getDeliveredOrders());
        report.setPendingOrders(stats.getPendingOrders());
        report.setLateDeliveries(stats.getLateDeliveries());
        report.setOnTimeDeliveryRate(stats.getOnTimeDeliveryRate());
        
        // Vehicle statistics
        report.setTotalDistanceTraveled(stats.getTotalDistanceTraveled());
        report.setTotalFuelConsumed(stats.getTotalFuelConsumed());
        report.setAverageFuelEfficiency(stats.getAverageFuelEfficiency());
        report.setTotalVehicleBreakdowns(stats.getTotalVehicleBreakdowns());
        report.setTotalMaintenanceEvents(stats.getTotalMaintenanceEvents());
        
        // Blockage statistics
        report.setTotalBlockages(stats.getTotalBlockages());
        report.setTotalBlockageDuration(stats.getTotalBlockageDuration());
        
        // Algorithm statistics
        report.setTotalReplans(stats.getTotalReplans());
        report.setAveragePlanningTimeMillis(stats.getAveragePlanningTimeMillis());
        
        // Calculate vehicle utilization statistics
        Map<String, SimulationStats.VehicleStats> vehicleStats = stats.getVehicleStats();
        report.setTotalVehicles(vehicleStats.size());
        
        double totalEfficiency = 0.0;
        for (SimulationStats.VehicleStats vs : vehicleStats.values()) {
            totalEfficiency += vs.getEfficiency();
        }
        double avgEfficiency = vehicleStats.size() > 0 ? totalEfficiency / vehicleStats.size() : 0.0;
        report.setAverageVehicleUtilization(avgEfficiency);
        
        return report;
    }
} 
package com.example.plgsystem.dto;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.simulation.SimulationState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class SimulationStateDTO {
    private String simulationId;
    private LocalDateTime currentTime;
    private List<VehicleDTO> vehicles;
    private DepotDTO mainDepot;
    private List<DepotDTO> auxDepots;
    private List<OrderDTO> orders;
    private List<Blockage> activeBlockages;
    private List<Incident> activeIncidents;
    private List<Maintenance> scheduledMaintenances;
    private int pendingOrdersCount;
    private int deliveredOrdersCount;
    private int overdueOrdersCount;
    private int availableVehiclesCount;
    private boolean isRunning;
    
    public static SimulationStateDTO fromSimulationState(
            String simulationId, 
            SimulationState state, 
            boolean isRunning) {
        
        SimulationStateDTO dto = new SimulationStateDTO();
        dto.setSimulationId(simulationId);
        dto.setCurrentTime(state.getCurrentTime());
        
        // Convert vehicles to DTOs
        dto.setVehicles(state.getVehicles().stream()
                .map(VehicleDTO::fromEntity)
                .collect(Collectors.toList()));
        
        // Convert depots to DTOs
        dto.setMainDepot(DepotDTO.fromEntity(state.getMainDepot()));
        dto.setAuxDepots(state.getAuxDepots().stream()
                .map(DepotDTO::fromEntity)
                .collect(Collectors.toList()));
        
        // Convert orders to DTOs
        dto.setOrders(state.getOrderQueue().stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList()));
        
        // Set active blockages
        dto.setActiveBlockages(state.getActiveBlockagesAt(state.getCurrentTime()));
        
        // Set counts
        dto.setPendingOrdersCount(state.getPendingOrders().size());
        dto.setDeliveredOrdersCount((int)state.getOrderQueue().stream().filter(Order::isDelivered).count());
        dto.setOverdueOrdersCount(state.getOverdueOrders().size());
        dto.setAvailableVehiclesCount(state.getAvailableVehicles().size());
        
        // Set running status
        dto.setRunning(isRunning);
        
        return dto;
    }
} 
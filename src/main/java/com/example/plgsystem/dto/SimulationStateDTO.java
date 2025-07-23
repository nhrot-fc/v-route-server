package com.example.plgsystem.dto;

import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.simulation.SimulationState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO para el estado de la simulación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationStateDTO {
    private String simulationId;
    private LocalDateTime currentTime;
    private SimulationStatus status;

    // Entidades principales
    private List<VehicleDTO> vehicles;
    private DepotDTO mainDepot;
    private List<DepotDTO> auxDepots;

    // Listas de entidades
    private List<OrderDTO> pendingOrders;
    private List<Blockage> activeBlockages;
    private List<IncidentDTO> activeIncidents;
    private List<MaintenanceDTO> scheduledMaintenances;
    private List<VehiclePlanDTO> currentVehiclePlans;
    private Map<String, LocalDateTime> maintenanceSchedule;

    // Estadísticas
    private int pendingOrdersCount;
    private int deliveredOrdersCount;
    private int overdueOrdersCount;
    private int availableVehiclesCount;

    /**
     * Convierte un estado de simulación a DTO
     * 
     * @param simulationId ID de la simulación
     * @param state        Estado de la simulación
     * @param status       Estado actual de ejecución
     * @return DTO con el estado de la simulación
     */
    public static SimulationStateDTO fromSimulationState(
            String simulationId,
            SimulationState state,
            SimulationStatus status) {

        // Create defensive copies of all collections to prevent concurrent modification
        List<Order> ordersCopy;
        List<Blockage> blockagesCopy;
        List<Vehicle> vehiclesCopy;
        List<Depot> auxDepotsCopy;
        List<Incident> incidentsCopy;
        List<Maintenance> maintenanceCopy;
        Map<String, VehiclePlan> vehiclePlansCopy;
        Map<String, LocalDateTime> maintenanceScheduleCopy;
        
        synchronized (state) {
            // Create copies of all collections
            ordersCopy = new ArrayList<>(state.getOrders());
            blockagesCopy = new ArrayList<>(state.getBlockages());
            vehiclesCopy = new ArrayList<>(state.getVehicles());
            auxDepotsCopy = new ArrayList<>(state.getAuxDepots());
            incidentsCopy = new ArrayList<>(state.getIncidents());
            maintenanceCopy = new ArrayList<>(state.getMaintenances());
            vehiclePlansCopy = new HashMap<>(state.getCurrentVehiclePlans());
            maintenanceScheduleCopy = new HashMap<>(state.getMaintenanceSchedule());
        }
        
        List<Order> pendingOrders = ordersCopy.stream()
                .filter(order -> order != null && !order.isDelivered())
                .toList();

        // Filtrar órdenes con entrega vencida
        List<Order> overdueOrders = ordersCopy.stream()
                .filter(order -> order != null && order.isOverdue(state.getCurrentTime()))
                .toList();

        // Filtrar bloqueos activos
        List<Blockage> activeBlockages = blockagesCopy.stream()
                .filter(blockage -> blockage.isActiveAt(state.getCurrentTime()))
                .toList();

        return SimulationStateDTO.builder()
                .simulationId(simulationId)
                .currentTime(state.getCurrentTime())
                .status(status)
                // Convertir vehículos a DTOs usando la copia local
                .vehicles(vehiclesCopy.stream()
                        .map(VehicleDTO::fromEntity)
                        .toList())
                // Convertir depósitos a DTOs
                .mainDepot(DepotDTO.fromEntity(state.getMainDepot()))
                .auxDepots(auxDepotsCopy.stream()
                        .map(DepotDTO::fromEntity)
                        .toList())
                // Convertir pedidos pendientes a DTOs
                .pendingOrders(pendingOrders.stream()
                        .map(OrderDTO::fromEntity)
                        .toList())
                // Establecer bloqueos activos
                .activeBlockages(activeBlockages)
                // Convertir incidentes a DTOs usando la copia local
                .activeIncidents(incidentsCopy.stream()
                        .filter(incident -> !incident.isResolved())
                        .map(IncidentDTO::fromEntity)
                        .toList())
                // Convertir mantenimientos a DTO usando la copia local
                .scheduledMaintenances(maintenanceCopy.stream()
                        .map(MaintenanceDTO::fromEntity)
                        .toList())
                // Agregar la programación de mantenimientos
                .maintenanceSchedule(maintenanceScheduleCopy)
                // Establecer contadores
                .pendingOrdersCount(pendingOrders.size())
                .deliveredOrdersCount((int) ordersCopy.stream()
                        .filter(Order::isDelivered).count())
                .overdueOrdersCount(overdueOrders.size())
                .availableVehiclesCount((int) vehiclesCopy.stream()
                        .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count())
                // Usar la copia local de planes de vehículos
                .currentVehiclePlans(vehiclePlansCopy.values().stream()
                        .map(VehiclePlanDTO::fromEntity)
                        .toList())
                .build();
    }
}
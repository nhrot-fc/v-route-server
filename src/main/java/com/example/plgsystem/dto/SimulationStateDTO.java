package com.example.plgsystem.dto;

import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.simulation.SimulationState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // Filtrar pedidos pendientes
        List<Order> pendingOrders = state.getOrders().stream()
                .filter(order -> order != null && !order.isDelivered() && !order.isOverdue(state.getCurrentTime()))
                .toList();

        // Filtrar órdenes con entrega vencida
        List<Order> overdueOrders = state.getOrders().stream()
                .filter(order -> order != null && order.isOverdue(state.getCurrentTime()) && !order.isDelivered())
                .toList();

        // Filtrar bloqueos activos
        List<Blockage> activeBlockages = state.getBlockages().stream()
                .filter(blockage -> blockage.isActiveAt(state.getCurrentTime()))
                .collect(Collectors.toList());

        return SimulationStateDTO.builder()
                .simulationId(simulationId)
                .currentTime(state.getCurrentTime())
                .status(status)
                // Convertir vehículos a DTOs
                .vehicles(state.getVehicles().stream()
                        .map(VehicleDTO::fromEntity)
                        .collect(Collectors.toList()))
                // Convertir depósitos a DTOs
                .mainDepot(DepotDTO.fromEntity(state.getMainDepot()))
                .auxDepots(state.getAuxDepots().stream()
                        .map(DepotDTO::fromEntity)
                        .collect(Collectors.toList()))
                // Convertir pedidos pendientes a DTOs
                .pendingOrders(pendingOrders.stream()
                        .map(OrderDTO::fromEntity)
                        .collect(Collectors.toList()))
                // Establecer bloqueos activos
                .activeBlockages(activeBlockages)
                // Convertir incidentes a DTOs
                .activeIncidents(state.getIncidents().stream()
                        .filter(incident -> !incident.isResolved())
                        .map(IncidentDTO::fromEntity)
                        .collect(Collectors.toList()))
                // Convertir mantenimientos a DTO
                .scheduledMaintenances(state.getMaintenances().stream()
                        .map(MaintenanceDTO::fromEntity)
                        .collect(Collectors.toList()))
                // Establecer contadores
                .pendingOrdersCount(pendingOrders.size())
                .deliveredOrdersCount((int) state.getOrders().stream()
                        .filter(Order::isDelivered).count())
                .overdueOrdersCount(overdueOrders.size())
                .availableVehiclesCount((int) state.getVehicles().stream()
                        .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count())
                .currentVehiclePlans(state.getCurrentVehiclePlans().values().stream()
                        .map(VehiclePlanDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}
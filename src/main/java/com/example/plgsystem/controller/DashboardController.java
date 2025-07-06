package com.example.plgsystem.controller;

import com.example.plgsystem.model.*;
import com.example.plgsystem.repository.*;
import com.example.plgsystem.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Panel de control y estadísticas del sistema PLG")
public class DashboardController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DepotRepository depotRepository;

    @Autowired
    private BlockageRepository blockageRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Operation(
        summary = "Obtener resumen del dashboard",
        description = "Proporciona estadísticas generales del sistema incluyendo vehículos, órdenes, depósitos y estado operacional"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Resumen del dashboard obtenido exitosamente",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/overview")
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        // Vehicle statistics
        List<Vehicle> availableVehicles = vehicleRepository.findByStatusOrderByCurrentGlpM3Desc(VehicleStatus.AVAILABLE);
        List<Vehicle> maintenanceVehicles = vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE);
        List<Vehicle> incidentVehicles = vehicleRepository.findByStatus(VehicleStatus.INCIDENT);
        
        overview.put("totalVehicles", vehicleRepository.count());
        overview.put("availableVehicles", availableVehicles.size());
        overview.put("vehiclesInMaintenance", maintenanceVehicles.size());
        overview.put("vehiclesWithIncidents", incidentVehicles.size());
        
        // Order statistics
        List<Order> pendingOrders = orderRepository.findPendingDeliveries();
        List<Order> completedOrders = orderRepository.findByRemainingGlpM3(0);
        List<Order> overdueOrders = orderRepository.findByDueTimeBefore(now)
            .stream()
            .filter(order -> order.getRemainingGlpM3() > 0)
            .collect(Collectors.toList());
        
        overview.put("totalOrders", orderRepository.count());
        overview.put("pendingOrders", pendingOrders.size());
        overview.put("completedOrders", completedOrders.size());
        overview.put("overdueOrders", overdueOrders.size());
        
        // Depot statistics
        List<Depot> depots = depotRepository.findAll();
        double totalStorageCapacity = depots.stream().mapToDouble(Depot::getGlpCapacityM3).sum();
        double currentTotalGLP = depots.stream().mapToDouble(Depot::getCurrentGlpM3).sum();
        
        overview.put("totalStorageCapacity", totalStorageCapacity);
        overview.put("currentTotalGLP", currentTotalGLP);
        overview.put("storageUtilization", 
                    totalStorageCapacity > 0 ? (currentTotalGLP / totalStorageCapacity * 100) : 0.0);
        
        // Fleet capacity statistics
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        double totalFleetCapacity = allVehicles.stream().mapToDouble(Vehicle::getGlpCapacityM3).sum();
        double availableFleetGLP = allVehicles.stream().mapToDouble(Vehicle::getCurrentGlpM3).sum();
        
        overview.put("totalFleetCapacity", totalFleetCapacity);
        overview.put("availableFleetGLP", availableFleetGLP);
        
        // Current operational status
        List<Blockage> activeBlockages = blockageRepository.findAll().stream()
            .filter(blockage -> blockage.isActiveAt(now))
            .collect(Collectors.toList());
        
        List<Maintenance> activeMaintenance = maintenanceRepository.findAll().stream()
            .filter(maintenance -> maintenance.getRealStart() != null && 
                   (maintenance.getRealEnd() == null || now.isBefore(maintenance.getRealEnd())))
            .collect(Collectors.toList());
            
        List<Incident> activeIncidents = incidentRepository.findAll().stream()
            .filter(incident -> !incident.isResolved())
            .collect(Collectors.toList());
        
        overview.put("activeBlockages", activeBlockages.size());
        overview.put("activeMaintenance", activeMaintenance.size());
        overview.put("activeIncidents", activeIncidents.size());
        
        overview.put("timestamp", now);
        
        return overview;
    }

    @Operation(
        summary = "Obtener estado de vehículos",
        description = "Devuelve un desglose de vehículos agrupados por su estado operativo"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado de vehículos obtenido exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Vehicle.class)
            )
        )
    })
    @GetMapping("/vehicle-status")
    public Map<String, List<Vehicle>> getVehicleStatusBreakdown() {
        Map<String, List<Vehicle>> vehicleStatus = new HashMap<>();
        
        vehicleStatus.put("available", vehicleRepository.findByStatus(VehicleStatus.AVAILABLE));
        vehicleStatus.put("maintenance", vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE));
        vehicleStatus.put("incident", vehicleRepository.findByStatus(VehicleStatus.INCIDENT));
        vehicleStatus.put("inRoute", vehicleRepository.findByStatus(VehicleStatus.DRIVING));
        vehicleStatus.put("delivering", vehicleRepository.findByStatus(VehicleStatus.SERVING));
        
        return vehicleStatus;
    }

    @Operation(
        summary = "Obtener órdenes urgentes",
        description = "Devuelve las órdenes que vencen dentro del plazo especificado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Órdenes urgentes obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Order.class)
            )
        )
    })
    @GetMapping("/urgent-orders")
    public List<Order> getUrgentOrders(
        @Parameter(description = "Horas de anticipación para considerar urgente", example = "4")
        @RequestParam(defaultValue = "4") int hoursAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(hoursAhead);
        
        return orderRepository.findPendingDeliveries().stream()
                .filter(order -> order.getDueTime().isBefore(deadline) && order.getDueTime().isAfter(now))
                .sorted((o1, o2) -> o1.getDueTime().compareTo(o2.getDueTime()))
                .collect(Collectors.toList());
    }

    @Operation(
        summary = "Obtener salud del sistema",
        description = "Calcula y devuelve puntuaciones de salud del sistema basadas en vehículos, incidentes y órdenes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Salud del sistema obtenida exitosamente",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/system-health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate overall system health score (0-100)
        int totalVehicles = (int) vehicleRepository.count();
        int availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).size();
        
        int activeIncidents = incidentRepository.findAll().stream()
            .filter(incident -> !incident.isResolved())
            .collect(Collectors.toList()).size();
            
        int overdueOrders = orderRepository.findByDueTimeBefore(now).stream()
            .filter(order -> order.getRemainingGlpM3() > 0)
            .collect(Collectors.toList()).size();
        
        double vehicleHealthScore = totalVehicles > 0 ? (double) availableVehicles / totalVehicles * 100 : 0;
        double incidentHealthScore = Math.max(0, 100 - (activeIncidents * 10)); // Each incident reduces score by 10
        double orderHealthScore = overdueOrders == 0 ? 100 : Math.max(0, 100 - (overdueOrders * 5)); // Each overdue order reduces score by 5
        
        double overallHealthScore = (vehicleHealthScore + incidentHealthScore + orderHealthScore) / 3;
        
        health.put("overallHealthScore", Math.round(overallHealthScore));
        health.put("vehicleHealthScore", Math.round(vehicleHealthScore));
        health.put("incidentHealthScore", Math.round(incidentHealthScore));
        health.put("orderHealthScore", Math.round(orderHealthScore));
        
        // Health status
        if (overallHealthScore >= 90) {
            health.put("status", "EXCELLENT");
        } else if (overallHealthScore >= 75) {
            health.put("status", "GOOD");
        } else if (overallHealthScore >= 50) {
            health.put("status", "FAIR");
        } else {
            health.put("status", "CRITICAL");
        }
        
        return health;
    }
}

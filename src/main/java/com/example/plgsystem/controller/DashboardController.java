package com.example.plgsystem.controller;

import com.example.plgsystem.model.*;
import com.example.plgsystem.repository.*;
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
        
        // Vehicle statistics
        List<Vehicle> availableVehicles = vehicleRepository.findAvailableVehicles();
        List<Vehicle> maintenanceVehicles = vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE);
        List<Vehicle> brokenVehicles = vehicleRepository.findByStatus(VehicleStatus.BROKEN_DOWN);
        
        overview.put("totalVehicles", vehicleRepository.count());
        overview.put("availableVehicles", availableVehicles.size());
        overview.put("vehiclesInMaintenance", maintenanceVehicles.size());
        overview.put("brokenVehicles", brokenVehicles.size());
        
        // Order statistics
        List<Order> pendingOrders = orderRepository.findPendingOrders();
        List<Order> completedOrders = orderRepository.findCompletedOrders();
        List<Order> overdueOrders = orderRepository.findOverdueOrders(LocalDateTime.now());
        
        overview.put("totalOrders", orderRepository.count());
        overview.put("pendingOrders", pendingOrders.size());
        overview.put("completedOrders", completedOrders.size());
        overview.put("overdueOrders", overdueOrders.size());
        
        // Depot statistics
        Double totalStorageCapacity = depotRepository.getTotalStorageCapacity();
        Double currentTotalGLP = depotRepository.getCurrentTotalGLP();
        
        overview.put("totalStorageCapacity", totalStorageCapacity != null ? totalStorageCapacity : 0.0);
        overview.put("currentTotalGLP", currentTotalGLP != null ? currentTotalGLP : 0.0);
        overview.put("storageUtilization", 
                    totalStorageCapacity != null && totalStorageCapacity > 0 
                    ? (currentTotalGLP != null ? currentTotalGLP / totalStorageCapacity * 100 : 0.0) 
                    : 0.0);
        
        // Fleet capacity statistics
        Double totalFleetCapacity = vehicleRepository.getTotalFleetCapacity();
        Double availableFleetGLP = vehicleRepository.getAvailableFleetGLP();
        
        overview.put("totalFleetCapacity", totalFleetCapacity != null ? totalFleetCapacity : 0.0);
        overview.put("availableFleetGLP", availableFleetGLP != null ? availableFleetGLP : 0.0);
        
        // Current operational status
        List<Blockage> activeBlockages = blockageRepository.findActiveBlockages(LocalDateTime.now());
        List<Maintenance> activeMaintenance = maintenanceRepository.findActiveMaintenance(LocalDateTime.now());
        List<Incident> activeIncidents = incidentRepository.findActiveIncidents(LocalDateTime.now());
        
        overview.put("activeBlockages", activeBlockages.size());
        overview.put("activeMaintenance", activeMaintenance.size());
        overview.put("activeIncidents", activeIncidents.size());
        
        overview.put("timestamp", LocalDateTime.now());
        
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
        vehicleStatus.put("brokenDown", vehicleRepository.findByStatus(VehicleStatus.BROKEN_DOWN));
        
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
        LocalDateTime deadline = LocalDateTime.now().plusHours(hoursAhead);
        return orderRepository.findUrgentOrders(deadline);
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
        
        // Calculate overall system health score (0-100)
        int totalVehicles = (int) vehicleRepository.count();
        int availableVehicles = vehicleRepository.findAvailableVehicles().size();
        int activeIncidents = incidentRepository.findActiveIncidents(LocalDateTime.now()).size();
        int overdueOrders = orderRepository.findOverdueOrders(LocalDateTime.now()).size();
        
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

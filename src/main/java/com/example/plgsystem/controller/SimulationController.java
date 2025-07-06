package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationReportDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "API para gestionar múltiples simulaciones")
public class SimulationController {

    private final SimulationManager simulationManager;
    private final ObjectMapper objectMapper;
    
    public SimulationController(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Operation(summary = "Listar todas las simulaciones", description = "Obtiene una lista de todas las simulaciones disponibles")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSimulations() {
        List<Map<String, Object>> simulations = simulationManager.getAllSimulations().stream()
            .map(simulation -> {
                Map<String, Object> simData = new HashMap<>();
                simData.put("id", simulation.getId());
                simData.put("name", simulation.getName());
                simData.put("description", simulation.getDescription());
                simData.put("createdAt", simulation.getCreatedAt());
                simData.put("lastUpdated", simulation.getLastUpdated());
                simData.put("currentTime", simulation.getState().getCurrentTime());
                return simData;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(simulations);
    }

    @Operation(summary = "Crear nueva simulación", description = "Crea una nueva instancia de simulación según el tipo especificado")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSimulation(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = true) String simulationType,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) Integer durationDays) {
        
        LocalDateTime startDateTime = startDate != null ? 
            LocalDateTime.parse(startDate) : LocalDateTime.now();
            
        // Create simulation with appropriate parameters based on type
        Simulation simulation;
        
        switch (simulationType.toLowerCase()) {
            case "daily":
                // Daily operations - use current data from database
                simulation = simulationManager.createSimulation(
                    name != null ? name : "Operaciones Diarias", 
                    description != null ? description : "Simulación con datos actuales", 
                    startDateTime,
                    "daily");
                break;
                
            case "weekly":
                // Weekly simulation - use data files for 7 days
                simulation = simulationManager.createSimulation(
                    name != null ? name : "Simulación Semanal", 
                    description != null ? description : "Simulación con datos históricos por 7 días", 
                    startDateTime,
                    "weekly");
                
                // Set simulation to run for 7 days maximum
                simulationManager.configureSimulation(simulation.getId(), dataSource, 7);
                break;
                
            case "collapse":
                // Collapse simulation - use all data files
                simulation = simulationManager.createSimulation(
                    name != null ? name : "Simulación hasta Colapso", 
                    description != null ? description : "Simulación con datos históricos hasta procesar todo", 
                    startDateTime,
                    "collapse");
                
                // Set simulation to run until all data is processed
                simulationManager.configureSimulation(simulation.getId(), dataSource, durationDays != null ? durationDays : 0);
                break;
                
            default:
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Tipo de simulación inválido. Use 'daily', 'weekly', o 'collapse'"
                ));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", simulation.getId());
        response.put("name", simulation.getName());
        response.put("description", simulation.getDescription());
        response.put("createdAt", simulation.getCreatedAt());
        response.put("simulationType", simulationType);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener estado de una simulación", description = "Obtiene el estado actual de una simulación específica")
    @GetMapping("/{id}/status")
    public ResponseEntity<SimulationStateDTO> getSimulationStatus(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        boolean isRunning = simulationManager.isSimulationRunning(id);
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
            simulation.getId(),
            simulation.getState(),
            isRunning
        );
        
        return ResponseEntity.ok(stateDTO);
    }

    @Operation(summary = "Obtener detalles del entorno", description = "Obtiene información detallada del entorno de simulación")
    @GetMapping("/{id}/environment")
    public ResponseEntity<SimulationStateDTO> getEnvironment(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        boolean isRunning = simulationManager.isSimulationRunning(id);
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
            simulation.getId(),
            simulation.getState(),
            isRunning
        );
        
        return ResponseEntity.ok(stateDTO);
    }
    
    @Operation(summary = "Obtener reporte de simulación", description = "Obtiene el reporte de una simulación finalizada")
    @GetMapping("/{id}/report")
    public ResponseEntity<SimulationReportDTO> getSimulationReport(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        Optional<SimulationReportDTO> reportOpt = simulationManager.getSimulationReport(id);
        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(reportOpt.get());
    }
    
    @Operation(summary = "Listar todos los reportes de simulación", description = "Obtiene todos los reportes de simulaciones finalizadas")
    @GetMapping("/reports")
    public ResponseEntity<List<SimulationReportDTO>> getAllReports() {
        List<SimulationReportDTO> reports = simulationManager.getAllSimulationReports();
        return ResponseEntity.ok(reports);
    }

    @Operation(summary = "Listar vehículos", description = "Obtiene la lista de vehículos en la simulación")
    @GetMapping("/{id}/vehicles")
    public ResponseEntity<List<Vehicle>> getVehicles(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @RequestParam(required = false) VehicleStatus status) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        List<Vehicle> vehicles = simulation.getState().getVehicles();
        if (status != null) {
            vehicles = vehicles.stream()
                .filter(v -> v.getStatus() == status)
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(vehicles);
    }

    @Operation(summary = "Listar órdenes", description = "Obtiene la lista de órdenes en la simulación")
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<Order>> getOrders(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false, defaultValue = "false") boolean overdueOnly) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        List<Order> orders;
        if (pendingOnly) {
            orders = simulation.getState().getPendingOrders();
        } else if (overdueOnly) {
            orders = simulation.getState().getOverdueOrders();
        } else {
            orders = simulation.getState().getOrderQueue();
        }
        
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Listar bloqueos", description = "Obtiene la lista de bloqueos activos en la simulación")
    @GetMapping("/{id}/blockages")
    public ResponseEntity<List<Blockage>> getBlockages(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        List<Blockage> blockages;
        if (activeOnly) {
            blockages = simulation.getState().getActiveBlockagesAt(
                simulation.getState().getCurrentTime());
        } else {
            blockages = simulation.getState().getBlockages();
        }
        
        return ResponseEntity.ok(blockages);
    }

    @Operation(summary = "Iniciar simulación", description = "Inicia o reanuda la ejecución de una simulación")
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startSimulation(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        boolean success = simulationManager.startSimulation(id);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", "running");
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Pausar simulación", description = "Pausa la ejecución de una simulación")
    @PostMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> pauseSimulation(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        boolean success = simulationManager.pauseSimulation(id);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", "paused");
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ajustar velocidad de simulación", description = "Ajusta la velocidad de ejecución de la simulación")
    @PostMapping("/{id}/speed")
    public ResponseEntity<Map<String, Object>> setSimulationSpeed(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @RequestParam int speedFactor) {
        
        boolean success = simulationManager.setSimulationSpeed(id, speedFactor);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("speedFactor", speedFactor);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simular avería de vehículo", description = "Simula una avería en un vehículo específico")
    @PostMapping("/{id}/vehicle/{vehicleId}/breakdown")
    public ResponseEntity<Map<String, Object>> simulateVehicleBreakdown(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        
        boolean success = simulationManager.simulateVehicleBreakdown(id, vehicleId);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("vehicleId", vehicleId);
        response.put("status", "breakdown");
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reparar vehículo", description = "Simula la reparación de un vehículo averiado")
    @PostMapping("/{id}/vehicle/{vehicleId}/repair")
    public ResponseEntity<Map<String, Object>> repairVehicle(
            @Parameter(description = "ID de la simulación") @PathVariable String id,
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        
        boolean success = simulationManager.repairVehicle(id, vehicleId);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("vehicleId", vehicleId);
        response.put("status", "repaired");
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar simulación", description = "Elimina una simulación existente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSimulation(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        boolean success = simulationManager.deleteSimulation(id);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
} 
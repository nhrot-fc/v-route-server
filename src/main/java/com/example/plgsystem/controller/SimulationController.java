package com.example.plgsystem.controller;

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

    @Operation(summary = "Crear nueva simulación", description = "Crea una nueva instancia de simulación")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSimulation(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String startDate) {
        
        LocalDateTime startDateTime = startDate != null ? 
            LocalDateTime.parse(startDate) : LocalDateTime.now();
            
        Simulation simulation = simulationManager.createSimulation(name, description, startDateTime);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", simulation.getId());
        response.put("name", simulation.getName());
        response.put("description", simulation.getDescription());
        response.put("createdAt", simulation.getCreatedAt());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener estado de una simulación", description = "Obtiene el estado actual de una simulación específica")
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getSimulationStatus(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        Map<String, Object> status = new HashMap<>();
        status.put("id", simulation.getId());
        status.put("name", simulation.getName());
        status.put("description", simulation.getDescription());
        status.put("createdAt", simulation.getCreatedAt());
        status.put("lastUpdated", simulation.getLastUpdated());
        status.put("currentTime", simulation.getState().getCurrentTime());
        status.put("isRunning", simulationManager.isSimulationRunning(id));
        status.put("pendingOrders", simulation.getState().getPendingOrders().size());
        status.put("activeVehicles", simulation.getState().getAvailableVehicles().size());
        status.put("activeBlockages", simulation.getState().getActiveBlockagesAt(
            simulation.getState().getCurrentTime()).size());
        
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Obtener detalles del entorno", description = "Obtiene información detallada del entorno de simulación")
    @GetMapping("/{id}/environment")
    public ResponseEntity<Map<String, Object>> getEnvironment(
            @Parameter(description = "ID de la simulación") @PathVariable String id) {
        
        Optional<Simulation> simulationOpt = simulationManager.getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Simulation simulation = simulationOpt.get();
        Map<String, Object> environment = new HashMap<>();
        environment.put("currentTime", simulation.getState().getCurrentTime());
        environment.put("mainDepot", simulation.getState().getMainDepot());
        environment.put("auxDepots", simulation.getState().getAuxDepots());
        environment.put("vehicleCount", simulation.getState().getVehicles().size());
        environment.put("orderCount", simulation.getState().getOrderQueue().size());
        environment.put("pendingOrders", simulation.getState().getPendingOrders().size());
        environment.put("activeBlockages", simulation.getState().getActiveBlockagesAt(
            simulation.getState().getCurrentTime()).size());
        
        return ResponseEntity.ok(environment);
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
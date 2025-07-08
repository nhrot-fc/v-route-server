package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationCreateDTO;
import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.service.SimulationService;
import com.example.plgsystem.simulation.Simulation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "API para gestionar múltiples simulaciones")
public class SimulationController {

    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
        logger.info("SimulationController initialized");
    }
    
    /**
     * WebSocket subscription handler for simulation updates
     * Client can subscribe to: /topic/simulation/{id}
     */
    @SubscribeMapping("/simulation/{id}")
    public SimulationDTO subscribeToSimulation(@DestinationVariable UUID id) {
        logger.info("WebSocket subscription to simulation updates with ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            logger.info("Returning simulation data for WebSocket subscription, simulation ID: {}", id);
            return new SimulationDTO(simulation);
        }
        logger.warn("WebSocket subscription failed: Simulation with ID {} not found", id);
        return null;
    }
    
    /**
     * WebSocket subscription handler for detailed simulation state
     * Client can subscribe to: /topic/simulation/{id}/state
     */
    @SubscribeMapping("/simulation/{id}/state")
    public SimulationStateDTO subscribeToSimulationState(@DestinationVariable UUID id) {
        logger.info("WebSocket subscription to simulation state with ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            logger.info("Returning simulation state for WebSocket subscription, simulation ID: {}", id);
            return SimulationStateDTO.fromSimulationState(
                    simulation.getId().toString(),
                    simulation.getState(),
                    simulation.getStatus());
        }
        logger.warn("WebSocket subscription failed: Simulation state with ID {} not found", id);
        return null;
    }
    
    /**
     * WebSocket message handler to force a simulation update
     * Client can send to: /app/simulation/{id}/update
     */
    @MessageMapping("/simulation/{id}/update")
    public void requestUpdate(@DestinationVariable UUID id) {
        logger.info("Received WebSocket request to update simulation with ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            logger.info("Sending forced simulation update for ID: {}", id);
            simulationService.sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot update: Simulation with ID {} not found", id);
        }
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily operations simulation", 
               description = "Returns the current status of the daily operations simulation")
    public ResponseEntity<SimulationDTO> getDailyOperations() {
        logger.info("Getting daily operations simulation");
        Simulation simulation = simulationService.getDailyOperations();
        if (simulation == null) {
            logger.warn("Daily operations simulation not found");
            return ResponseEntity.notFound().build();
        }
        logger.info("Returning daily operations simulation with ID: {}", simulation.getId());
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }

    @GetMapping("/daily/state")
    @Operation(summary = "Get daily operations state", 
               description = "Returns the current detailed state of the daily operations simulation")
    public ResponseEntity<SimulationStateDTO> getDailyOperationsState() {
        logger.info("Getting daily operations simulation state");
        Simulation simulation = simulationService.getDailyOperations();
        if (simulation == null) {
            logger.warn("Daily operations simulation not found for state request");
            return ResponseEntity.notFound().build();
        }
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                simulation.getId().toString(), 
                simulation.getState(), 
                simulation.getStatus());
        
        logger.info("Returning daily operations simulation state with ID: {}", simulation.getId());
        return ResponseEntity.ok(stateDTO);
    }

    @PostMapping
    @Operation(summary = "Create a new simplified simulation", 
               description = "Creates a new simulation with the specified parameters. For WEEKLY type, end date is automatically set to one week after start date. For INFINITE, no end date is used.")
    @ApiResponse(responseCode = "201", description = "Simulation created successfully")
    public ResponseEntity<SimulationDTO> createSimulation(@RequestBody SimulationCreateDTO createDTO) {
        logger.info("Creating new simulation of type: {}, start time: {}, vehicles: TA={}, TB={}, TC={}, TD={}",
                createDTO.getType(), createDTO.getStartDateTime(), 
                createDTO.getTaVehicles(), createDTO.getTbVehicles(), 
                createDTO.getTcVehicles(), createDTO.getTdVehicles());
        
        // Validate simulation type
        SimulationType type = createDTO.getType();
        if (type.isDailyOperation()) {
            logger.warn("Cannot create daily operation simulation. Daily operations can only be accessed, not created.");
            return ResponseEntity.badRequest().build();
        }
        
        // Validate vehicle counts
        if (createDTO.getTaVehicles() < 0 || createDTO.getTbVehicles() < 0 || 
            createDTO.getTcVehicles() < 0 || createDTO.getTdVehicles() < 0) {
            logger.warn("Invalid vehicle counts: all counts must be non-negative");
            return ResponseEntity.badRequest().build();
        }
        
        // Validate dates based on simulation type
        if (createDTO.getStartDateTime() == null) {
            logger.warn("Start date is required");
            return ResponseEntity.badRequest().build();
        }
        
        if (type == SimulationType.CUSTOM && createDTO.getEndDateTime() == null) {
            logger.warn("End date is required for CUSTOM simulation type");
            return ResponseEntity.badRequest().build();
        }
        
        // Create the simulation
        Simulation simulation = simulationService.createSimplifiedSimulation(
                createDTO.getType(),
                createDTO.getStartDateTime(),
                createDTO.getEndDateTime(),
                createDTO.getTaVehicles(),
                createDTO.getTbVehicles(),
                createDTO.getTcVehicles(),
                createDTO.getTdVehicles());
        
        logger.info("Simulation created with ID: {}", simulation.getId());
        return new ResponseEntity<>(new SimulationDTO(simulation), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get simulation status", 
               description = "Returns the current status of a specific simulation")
    public ResponseEntity<SimulationDTO> getSimulationStatus(@PathVariable UUID id) {
        logger.info("Getting simulation status for ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation == null) {
            logger.warn("Simulation with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
        logger.info("Returning simulation status for ID: {}", id);
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @GetMapping("/{id}/state")
    @Operation(summary = "Get simulation state", 
               description = "Returns the current detailed state of a specific simulation")
    public ResponseEntity<SimulationStateDTO> getSimulationState(@PathVariable UUID id) {
        logger.info("Getting simulation state for ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation == null) {
            logger.warn("Simulation with ID {} not found for state request", id);
            return ResponseEntity.notFound().build();
        }
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                simulation.getId().toString(), 
                simulation.getState(), 
                simulation.getStatus());
        
        logger.info("Returning simulation state for ID: {}", id);
        return ResponseEntity.ok(stateDTO);
    }
    
    @GetMapping
    @Operation(summary = "List all simulations", 
               description = "Returns a list of all active simulations")
    public ResponseEntity<Map<UUID, SimulationDTO>> listSimulations() {
        logger.info("Listing all simulations");
        Map<UUID, Simulation> simulations = simulationService.getAllSimulations();
        Map<UUID, SimulationDTO> dtos = simulations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SimulationDTO(e.getValue())
                ));
        logger.info("Found {} active simulations", dtos.size());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/{id}/start")
    @Operation(summary = "Start a simulation", 
               description = "Starts or resumes a paused simulation")
    public ResponseEntity<SimulationDTO> startSimulation(@PathVariable UUID id) {
        logger.info("Starting/resuming simulation with ID: {}", id);
        Simulation simulation = simulationService.startSimulation(id);
        if (simulation == null) {
            logger.warn("Cannot start: Simulation with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
        logger.info("Simulation with ID: {} successfully started", id);
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a simulation", 
               description = "Pauses a running simulation")
    public ResponseEntity<SimulationDTO> pauseSimulation(@PathVariable UUID id) {
        logger.info("Pausing simulation with ID: {}", id);
        Simulation simulation = simulationService.pauseSimulation(id);
        if (simulation == null) {
            logger.warn("Cannot pause: Simulation with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
        logger.info("Simulation with ID: {} successfully paused", id);
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop a simulation", 
               description = "Permanently stops a simulation")
    public ResponseEntity<SimulationDTO> stopSimulation(@PathVariable UUID id) {
        logger.info("Stopping simulation with ID: {}", id);
        Simulation simulation = simulationService.finishSimulation(id);
        if (simulation == null) {
            logger.warn("Cannot stop: Simulation with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
        logger.info("Simulation with ID: {} successfully stopped", id);
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
} 
package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationCreateDTO;
import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.DepotService;
import com.example.plgsystem.service.SimulationService;
import com.example.plgsystem.service.VehicleService;
import com.example.plgsystem.simulation.Simulation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "API para gestionar múltiples simulaciones")
public class SimulationController {

    private final SimulationService simulationService;
    private final VehicleService vehicleService;
    private final DepotService depotService;

    public SimulationController(SimulationService simulationService, 
                               VehicleService vehicleService, 
                               DepotService depotService) {
        this.simulationService = simulationService;
        this.vehicleService = vehicleService;
        this.depotService = depotService;
    }
    
    /**
     * WebSocket subscription handler for simulation updates
     * Client can subscribe to: /topic/simulation/{id}
     */
    @SubscribeMapping("/simulation/{id}")
    public SimulationDTO subscribeToSimulation(@DestinationVariable UUID id) {
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            return new SimulationDTO(simulation);
        }
        return null;
    }
    
    /**
     * WebSocket subscription handler for detailed simulation state
     * Client can subscribe to: /topic/simulation/{id}/state
     */
    @SubscribeMapping("/simulation/{id}/state")
    public SimulationStateDTO subscribeToSimulationState(@DestinationVariable UUID id) {
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            return SimulationStateDTO.fromSimulationState(
                    simulation.getId().toString(),
                    simulation.getState(),
                    simulation.getStatus());
        }
        return null;
    }
    
    /**
     * WebSocket message handler to force a simulation update
     * Client can send to: /app/simulation/{id}/update
     */
    @MessageMapping("/simulation/{id}/update")
    public void requestUpdate(@DestinationVariable UUID id) {
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation != null) {
            simulationService.sendSimulationUpdate(simulation);
        }
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily operations simulation", 
               description = "Returns the current status of the daily operations simulation")
    public ResponseEntity<SimulationDTO> getDailyOperations() {
        Simulation simulation = simulationService.getDailyOperations();
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }

    @GetMapping("/daily/state")
    @Operation(summary = "Get daily operations state", 
               description = "Returns the current detailed state of the daily operations simulation")
    public ResponseEntity<SimulationStateDTO> getDailyOperationsState() {
        Simulation simulation = simulationService.getDailyOperations();
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                simulation.getId().toString(), 
                simulation.getState(), 
                simulation.getStatus());
        
        return ResponseEntity.ok(stateDTO);
    }

    @PostMapping
    @Operation(summary = "Create a new time-based simulation", 
               description = "Creates a new simulation with the specified parameters")
    @ApiResponse(responseCode = "201", description = "Simulation created successfully")
    public ResponseEntity<SimulationDTO> createSimulation(@RequestBody SimulationCreateDTO createDTO, 
                                                         @RequestParam(defaultValue = "CUSTOM") SimulationType type) {
        // Retrieve vehicles from IDs
        List<Vehicle> vehicles = createDTO.getVehicleIds().stream()
                .map(id -> vehicleService.findById(id).orElse(null))
                .filter(vehicle -> vehicle != null)
                .collect(Collectors.toList());
                
        // Retrieve main depot
        Depot mainDepot = depotService.findById(createDTO.getMainDepotId()).orElse(null);
        if (mainDepot == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Retrieve auxiliary depots
        List<Depot> auxDepots = createDTO.getAuxDepotIds().stream()
                .map(id -> depotService.findById(id).orElse(null))
                .filter(depot -> depot != null)
                .collect(Collectors.toList());
                
        // Create time-based simulation
        Simulation simulation = simulationService.createTimeBasedSimulation(
                type.isTimeBasedSimulation() ? type : SimulationType.CUSTOM,
                vehicles, 
                mainDepot, 
                auxDepots, 
                createDTO.getStartDateTime());
        
        return new ResponseEntity<>(new SimulationDTO(simulation), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get simulation status", 
               description = "Returns the current status of a specific simulation")
    public ResponseEntity<SimulationDTO> getSimulationStatus(@PathVariable UUID id) {
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @GetMapping("/{id}/state")
    @Operation(summary = "Get simulation state", 
               description = "Returns the current detailed state of a specific simulation")
    public ResponseEntity<SimulationStateDTO> getSimulationState(@PathVariable UUID id) {
        Simulation simulation = simulationService.getSimulation(id);
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                simulation.getId().toString(), 
                simulation.getState(), 
                simulation.getStatus());
        
        return ResponseEntity.ok(stateDTO);
    }
    
    @GetMapping
    @Operation(summary = "List all simulations", 
               description = "Returns a list of all active simulations")
    public ResponseEntity<Map<UUID, SimulationDTO>> listSimulations() {
        Map<UUID, Simulation> simulations = simulationService.getAllSimulations();
        Map<UUID, SimulationDTO> dtos = simulations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SimulationDTO(e.getValue())
                ));
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/{id}/start")
    @Operation(summary = "Start a simulation", 
               description = "Starts or resumes a paused simulation")
    public ResponseEntity<SimulationDTO> startSimulation(@PathVariable UUID id) {
        Simulation simulation = simulationService.startSimulation(id);
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a simulation", 
               description = "Pauses a running simulation")
    public ResponseEntity<SimulationDTO> pauseSimulation(@PathVariable UUID id) {
        Simulation simulation = simulationService.pauseSimulation(id);
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
    
    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop a simulation", 
               description = "Permanently stops a simulation")
    public ResponseEntity<SimulationDTO> stopSimulation(@PathVariable UUID id) {
        Simulation simulation = simulationService.finishSimulation(id);
        if (simulation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }
} 
package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.dto.SimulationCreateDTO;
import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.service.SimulationService;
import com.example.plgsystem.simulation.Simulation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping
    @Operation(summary = "Create a new simplified simulation", description = "Creates a new simulation with the specified parameters. For WEEKLY type, end date is automatically set to one week after start date. For INFINITE, no end date is used.")
    @ApiResponse(responseCode = "201", description = "Simulation created successfully")
    public ResponseEntity<SimulationDTO> createSimulation(@RequestBody SimulationCreateDTO createDTO) {
        logger.info("Creating new simulation of type: {}, start time: {}, vehicles: TA={}, TB={}, TC={}, TD={}",
                createDTO.getType(), createDTO.getStartDateTime(),
                createDTO.getTaVehicles(), createDTO.getTbVehicles(),
                createDTO.getTcVehicles(), createDTO.getTdVehicles());

        // Validate simulation type
        SimulationType type = createDTO.getType();
        if (type.isDailyOperation()) {
            logger.warn(
                    "Cannot create daily operation simulation. Daily operations can only be accessed, not created.");
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
        Simulation simulation = simulationService.createSimulation(
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

    @GetMapping
    @Operation(summary = "List all simulations", description = "Returns a list of all active simulations")
    public ResponseEntity<Map<UUID, SimulationDTO>> listSimulations() {
        Map<UUID, Simulation> simulations = simulationService.getAllSimulations();
        Map<UUID, SimulationDTO> simulationDTOList = simulations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SimulationDTO(e.getValue())));
        logger.info("Found {} active simulations", simulationDTOList.size());
        return ResponseEntity.ok(simulationDTOList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get simulation by ID", description = "Returns a specific simulation by its ID")
    public ResponseEntity<SimulationDTO> getSimulation(@PathVariable UUID id) {
        logger.info("Retrieving simulation with ID: {}", id);
        Simulation simulation = simulationService.getSimulation(id);
        
        if (simulation == null) {
            logger.warn("Simulation with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
        
        logger.info("Retrieved simulation with ID: {}, Type: {}, Status: {}", 
                simulation.getId(), simulation.getType(), simulation.getStatus());
        return ResponseEntity.ok(new SimulationDTO(simulation));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a simulation", description = "Starts or resumes a paused simulation")
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
    @Operation(summary = "Pause a simulation", description = "Pauses a running simulation")
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
    @Operation(summary = "Stop a simulation", description = "Permanently stops a simulation")
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

    @PostMapping(value = "/{id}/load-orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar órdenes para una simulación", description = "Carga un archivo de órdenes para un año y mes específico en una simulación")
    public ResponseEntity<String> loadOrders(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam MultipartFile file) {

        logger.info("Loading orders for simulation with ID: {}, year: {}, month: {}", id, year, month);

        try {
            if (file.isEmpty()) {
                logger.warn("Cannot load orders: The file is empty for simulation ID {}", id);
                return ResponseEntity.badRequest().body("Error: El archivo no puede estar vacío.");
            }

            Simulation simulation = simulationService.getSimulation(id);
            if (simulation == null) {
                logger.warn("Cannot load orders: Simulation with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }

            simulationService.loadOrders(simulation, year, month, file);
            logger.info("Orders loaded for simulation with ID: {}", id);
            return ResponseEntity.ok("Órdenes cargadas exitosamente para la simulación con ID: " + id);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for orders loading: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error en los parámetros: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error loading orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cargar órdenes: " + e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/load-blockages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar bloqueos para una simulación", description = "Carga un archivo de bloqueos para un año y mes específico en una simulación")
    public ResponseEntity<String> loadBlockages(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam MultipartFile file) {

        logger.info("Loading blockages for simulation with ID: {}, year: {}, month: {}", id, year, month);

        try {
            Simulation simulation = simulationService.getSimulation(id);
            if (simulation == null) {
                logger.warn("Cannot load blockages: Simulation with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }

            simulationService.loadBlockages(simulation, year, month, file);

            logger.info("Blockages loaded for simulation with ID: {}", id);
            return ResponseEntity.ok("Bloqueos cargados exitosamente para la simulación con ID: " + id);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for blockages loading: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error en los parámetros: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error loading blockages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cargar bloqueos: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una simulación", description = "Elimina una simulación por su ID")
    public ResponseEntity<Void> deleteSimulation(@PathVariable UUID id) {
        logger.info("Eliminando simulación con ID: {}", id);

        Simulation simulation = simulationService.getSimulation(id);
        if (simulation == null) {
            logger.warn("No se encontró la simulación con ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        simulationService.deleteSimulation(id);
        logger.info("Simulación con ID: {} eliminada exitosamente", id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/speed/{factor}")
    @Operation(summary = "Configurar velocidad de simulación", description = "Configura la velocidad de todas las simulaciones (1 = normal, 2-5 = velocidades más rápidas)")
    public ResponseEntity<Map<String, Object>> setSimulationSpeed(@PathVariable int factor) {
        logger.info("Configurando velocidad de simulación a factor: {}", factor);
        
        // Validar que el factor esté entre 1 y 5
        if (factor < 1) {
            logger.warn("Factor de velocidad inválido: {}. Debe ser 1 o más.", factor);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Factor de velocidad inválido. Debe ser 1 o más.",
                "factorRecibido", factor
            ));
        }
        
        try {
            simulationService.setSimulationFrequency(factor);
            
            String velocidad;
            switch (factor) {
                case 1: velocidad = "normal (1x)"; break;
                case 2: velocidad = "rápida (2x)"; break;
                case 3: velocidad = "muy rápida (3x)"; break;
                case 4: velocidad = "ultra rápida (4x)"; break;
                case 5: velocidad = "máxima (5x)"; break;
                default: velocidad = "extrema (más de 5x)"; break;
            }
            
            return ResponseEntity.ok().body(Map.of(
                "message", "Velocidad de simulación configurada correctamente",
                "factor", factor,
                "velocidad", velocidad
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Error al configurar velocidad de simulación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "factorRecibido", factor
            ));
        }
    }

    @PostMapping("/{simulationId}/vehicle/{vehicleId}/breakdown")
    @Operation(summary = "Crear avería de vehículo", description = "Crea un evento de avería para un vehículo en la simulación")
    public ResponseEntity<?> createVehicleBreakdown(
            @PathVariable UUID simulationId,
            @PathVariable String vehicleId,
            @RequestBody(required = false) IncidentCreateDTO incidentDTO) {
        
        logger.info("Creando avería para vehículo {} en simulación {}", vehicleId, simulationId);
        
        try {
            Simulation simulation = simulationService.getSimulation(simulationId);
            if (simulation == null) {
                logger.warn("No se encontró la simulación con ID: {}", simulationId);
                return ResponseEntity.notFound().build();
            }
            
            // Don't allow incidents in benchmark simulations
            if (simulation.getType() == SimulationType.INFINITE) {
                logger.warn("No se permiten averías en simulaciones de tipo INFINITE");
                return ResponseEntity.badRequest().body("No se permiten averías en simulaciones de tipo INFINITE");
            }
            
            // Initialize DTO if not provided
            if (incidentDTO == null) {
                incidentDTO = new IncidentCreateDTO();
            }
            
            // Set vehicle ID
            incidentDTO.setVehicleId(vehicleId);
            
            // Set default type if not specified
            if (incidentDTO.getType() == null) {
                incidentDTO.setType(IncidentType.TI1);
            }
            
            // Set current time if not specified
            if (incidentDTO.getOccurrenceTime() == null) {
                incidentDTO.setOccurrenceTime(simulation.getState().getCurrentTime());
            }
            
            simulationService.createVehicleBreakdown(simulation, incidentDTO);
            
            logger.info("Avería creada exitosamente para vehículo {} en simulación {}, tipo: {}", 
                    vehicleId, simulationId, incidentDTO.getType());
            return ResponseEntity.ok().body(Map.of(
                "message", "Avería creada exitosamente",
                "vehicleId", vehicleId,
                "simulationId", simulationId,
                "type", incidentDTO.getType(),
                "occurrenceTime", incidentDTO.getOccurrenceTime()
            ));
            
        } catch (IllegalArgumentException e) {
            logger.error("Error al crear avería: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al crear avería: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear avería: " + e.getMessage());
        }
    }

}
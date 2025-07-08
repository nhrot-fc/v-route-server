package com.example.plgsystem.service;

import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulationService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    
    private final Map<UUID, Simulation> simulations = new ConcurrentHashMap<>();
    private UUID dailyOperationsId;

    private final DepotService depotService;
    private final VehicleService vehicleService;
    private final SimpMessagingTemplate messagingTemplate;

    public SimulationService(DepotService depotService, VehicleService vehicleService,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.depotService = depotService;
        this.vehicleService = vehicleService;
        this.messagingTemplate = messagingTemplate;
        logger.info("SimulationService initialized");
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        logger.info("Application context refreshed, initializing daily operations");
        // Initialize the daily operations simulation when the application starts
        initializeDailyOperations();
    }

    /**
     * Initializes the daily operations simulation using current database state
     */
    private void initializeDailyOperations() {
        logger.info("Initializing daily operations simulation");
        // Get main depot
        Depot mainDepot = depotService.findMainDepots().stream().findFirst().orElse(null);
        if (mainDepot == null) {
            mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
            depotService.save(mainDepot);
        }

        // Get all vehicles
        List<Vehicle> vehicles = vehicleService.findAll();
        logger.info("Found {} vehicles for daily operations", vehicles.size());

        // Get auxiliary depots
        List<Depot> auxDepots = depotService.findAuxiliaryDepots();
        if (auxDepots.isEmpty()) {
            auxDepots = Arrays.asList(
                new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 500, DepotType.AUXILIARY),
                new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 500, DepotType.AUXILIARY)
            );
            for (Depot depot : auxDepots) {
                depotService.save(depot);
            }
        }

        logger.info("Found {} auxiliary depots for daily operations", auxDepots.size());

        // Create simulation state with current time
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());

        // Create daily operations simulation
        Simulation dailyOps = new Simulation(state, SimulationType.DAILY_OPERATIONS);
        dailyOps.start(); // Start it immediately

        // Store in simulations map
        dailyOperationsId = dailyOps.getId();
        simulations.put(dailyOperationsId, dailyOps);
        logger.info("Daily operations simulation created with ID: {}", dailyOperationsId);

        // Send initial state to all WebSocket subscribers
        sendSimulationUpdate(dailyOps);
    }

    /**
     * Creates a new time-based simulation
     */
    public Simulation createTimeBasedSimulation(SimulationType type, SimulationState state) {
        logger.info("Creating time-based simulation of type: {}", type);
        if (type.isDailyOperation()) {
            logger.error("Cannot create additional daily operation simulations");
            throw new IllegalArgumentException("Cannot create additional daily operation simulations");
        }

        Simulation simulation = new Simulation(state, type);
        simulations.put(simulation.getId(), simulation);
        logger.info("Created time-based simulation with ID: {}", simulation.getId());

        // Broadcast the initial state to create the channel
        sendSimulationUpdate(simulation);
        
        return simulation;
    }

    /**
     * Creates a simplified simulation with automatic generation of vehicles
     * 
     * @param type Type of simulation (WEEKLY, INFINITE, CUSTOM)
     * @param startDateTime Starting date and time of the simulation
     * @param endDateTime End date for CUSTOM simulations, ignored for other types
     * @param taVehicleCount Number of TA vehicles to create
     * @param tbVehicleCount Number of TB vehicles to create
     * @param tcVehicleCount Number of TC vehicles to create
     * @param tdVehicleCount Number of TD vehicles to create
     * @return The created simulation
     */
    public Simulation createSimplifiedSimulation(
            SimulationType type,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int taVehicleCount,
            int tbVehicleCount,
            int tcVehicleCount,
            int tdVehicleCount) {
        
        logger.info("Creating simplified simulation - type: {}, start: {}, end: {}, vehicles: TA={}, TB={}, TC={}, TD={}",
            type, startDateTime, endDateTime, taVehicleCount, tbVehicleCount, tcVehicleCount, tdVehicleCount);
        
        if (type.isDailyOperation()) {
            logger.error("Cannot create additional daily operation simulations");
            throw new IllegalArgumentException("Cannot create additional daily operation simulations");
        }
        
        // For WEEKLY type, set end date automatically to one week after start
        if (type == SimulationType.WEEKLY) {
            endDateTime = startDateTime.plusWeeks(1);
            logger.info("Weekly simulation: auto-set end date to {}", endDateTime);
        }
        
        // Get fixed depots from the database
        Depot mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
        Depot northDepot = new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 500, DepotType.AUXILIARY);
        Depot eastDepot = new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 500, DepotType.AUXILIARY);
        List<Depot> auxDepots = Arrays.asList(northDepot, eastDepot);
        logger.info("Using fixed depots: Main={}, Aux=[{}, {}]", mainDepot.getId(), northDepot.getId(), eastDepot.getId());
        
        // Create vehicles automatically
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Generate TA vehicles
        for (int i = 0; i < taVehicleCount; i++) {
            String id = "TA" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TA)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }
        
        // Generate TB vehicles
        for (int i = 0; i < tbVehicleCount; i++) {
            String id = "TB" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TB)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }
        
        // Generate TC vehicles
        for (int i = 0; i < tcVehicleCount; i++) {
            String id = "TC" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TC)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }
        
        // Generate TD vehicles
        for (int i = 0; i < tdVehicleCount; i++) {
            String id = "TD" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TD)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }
        
        logger.info("Created {} vehicles for simulation", vehicles.size());
        
        // Create simulation state
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startDateTime);
        Simulation simulation = new Simulation(state, type);
        
        // Store in simulations map
        simulations.put(simulation.getId(), simulation);
        logger.info("Created simulation with ID: {}", simulation.getId());
        
        // Broadcast the initial state to create the channel
        sendSimulationUpdate(simulation);
        
        return simulation;
    }

    /**
     * Creates a new time-based simulation with specific vehicles and depots
     */
    public Simulation createTimeBasedSimulation(SimulationType type, List<Vehicle> vehicles,
            Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime startTime) {
        logger.info("Creating time-based simulation with {} vehicles, start time: {}", vehicles.size(), startTime);
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startTime);
        return createTimeBasedSimulation(type, state);
    }

    /**
     * Retrieves a simulation by its ID
     */
    public Simulation getSimulation(UUID id) {
        logger.debug("Getting simulation with ID: {}", id);
        return simulations.get(id);
    }

    /**
     * Gets the daily operations simulation
     */
    public Simulation getDailyOperations() {
        logger.debug("Getting daily operations simulation with ID: {}", dailyOperationsId);
        return simulations.get(dailyOperationsId);
    }

    /**
     * Starts or resumes a simulation
     */
    public Simulation startSimulation(UUID id) {
        logger.info("Starting/resuming simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.start();
            logger.info("Simulation {} started successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot start simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Pauses a running simulation
     */
    public Simulation pauseSimulation(UUID id) {
        logger.info("Pausing simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.pause();
            logger.info("Simulation {} paused successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot pause simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Finishes a simulation
     */
    public Simulation finishSimulation(UUID id) {
        logger.info("Finishing simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.finish();
            logger.info("Simulation {} finished successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot finish simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Retrieves all active simulations
     */
    public Map<UUID, Simulation> getAllSimulations() {
        logger.debug("Getting all simulations, count: {}", simulations.size());
        return simulations;
    }

    /**
     * Updates simulation states and broadcasts updates to WebSocket channels
     * Runs every 1 seconds for all active simulations
     */
    @Scheduled(fixedRate = 1000)
    public void updateAndBroadcastSimulations() {
        logger.trace("Updating and broadcasting all running simulations");
        
        int runningCount = 0;
        for (Simulation simulation : simulations.values()) {
            // Only update running simulations
            if (simulation.isRunning()) {
                runningCount++;
                
                // For daily operations, update to current time
                if (simulation.isDailyOperation()) {
                    // Update the simulation state with current time
                    simulation.getState().setCurrentTime(LocalDateTime.now());
                } else {
                    // For time-based simulations, advance time by a specific interval
                    // This could be configured based on simulation type
                    Duration timeStep = Duration.ofMinutes(1);
                    simulation.getState().advanceTime(timeStep);
                }

                // Broadcast updates to the WebSocket channel
                sendSimulationUpdate(simulation);
            }
        }
        
        if (runningCount > 0) {
            logger.debug("Updated {} running simulations", runningCount);
        }
    }

    /**
     * Sends simulation updates to the WebSocket channels
     */
    public void sendSimulationUpdate(Simulation simulation) {
        logger.trace("Sending WebSocket update for simulation ID: {}", simulation.getId());
        
        UUID id = simulation.getId();
        String channelBasePath = "/topic/simulation/" + id;

        // Send basic simulation data on the base channel
        messagingTemplate.convertAndSend(
                channelBasePath,
                new SimulationDTO(simulation));

        // Send detailed state data on the state channel
        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                id.toString(),
                simulation.getState(),
                simulation.getStatus());

        messagingTemplate.convertAndSend(
                channelBasePath + "/state",
                stateDTO);
    }
}
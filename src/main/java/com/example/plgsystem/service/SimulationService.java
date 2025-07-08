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
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // Initialize the daily operations simulation when the application starts
        initializeDailyOperations();
    }

    /**
     * Initializes the daily operations simulation using current database state
     */
    private void initializeDailyOperations() {
        // Get main depot
        Depot mainDepot = depotService.findMainDepots().stream().findFirst().orElse(null);
        if (mainDepot == null) {
            // Can't initialize without a main depot
            return;
        }

        // Get all vehicles
        List<Vehicle> vehicles = vehicleService.findAll();

        // Get auxiliary depots
        List<Depot> auxDepots = depotService.findAuxiliaryDepots();

        // Create simulation state with current time
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());

        // Create daily operations simulation
        Simulation dailyOps = new Simulation(state, SimulationType.DAILY_OPERATIONS);
        dailyOps.start(); // Start it immediately

        // Store in simulations map
        dailyOperationsId = dailyOps.getId();
        simulations.put(dailyOperationsId, dailyOps);

        // Send initial state to all WebSocket subscribers
        sendSimulationUpdate(dailyOps);
    }

    /**
     * Creates a new time-based simulation
     */
    public Simulation createTimeBasedSimulation(SimulationType type, SimulationState state) {
        if (type.isDailyOperation()) {
            throw new IllegalArgumentException("Cannot create additional daily operation simulations");
        }

        Simulation simulation = new Simulation(state, type);
        simulations.put(simulation.getId(), simulation);

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
        
        if (type.isDailyOperation()) {
            throw new IllegalArgumentException("Cannot create additional daily operation simulations");
        }
        
        // For WEEKLY type, set end date automatically to one week after start
        if (type == SimulationType.WEEKLY) {
            endDateTime = startDateTime.plusWeeks(1);
        }
        
        // Get fixed depots from the database
        Depot mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 1000, DepotType.MAIN);
        Depot northDepot = new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 1000, DepotType.AUXILIARY);
        Depot eastDepot = new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 1000, DepotType.AUXILIARY);
        List<Depot> auxDepots = Arrays.asList(northDepot, eastDepot);
        
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
        
        // Create simulation state
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startDateTime);
        Simulation simulation = new Simulation(state, type);
        
        // Store in simulations map
        simulations.put(simulation.getId(), simulation);
        
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
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startTime);
        return createTimeBasedSimulation(type, state);
    }

    /**
     * Retrieves a simulation by its ID
     */
    public Simulation getSimulation(UUID id) {
        return simulations.get(id);
    }

    /**
     * Gets the daily operations simulation
     */
    public Simulation getDailyOperations() {
        return simulations.get(dailyOperationsId);
    }

    /**
     * Starts or resumes a simulation
     */
    public Simulation startSimulation(UUID id) {
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.start();
            sendSimulationUpdate(simulation);
        }
        return simulation;
    }

    /**
     * Pauses a running simulation
     */
    public Simulation pauseSimulation(UUID id) {
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.pause();
            sendSimulationUpdate(simulation);
        }
        return simulation;
    }

    /**
     * Finishes a simulation
     */
    public Simulation finishSimulation(UUID id) {
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.finish();
            sendSimulationUpdate(simulation);
        }
        return simulation;
    }

    /**
     * Retrieves all active simulations
     */
    public Map<UUID, Simulation> getAllSimulations() {
        return simulations;
    }

    /**
     * Updates simulation states and broadcasts updates to WebSocket channels
     * Runs every 1 seconds for all active simulations
     */
    @Scheduled(fixedRate = 1000)
    public void updateAndBroadcastSimulations() {
        simulations.values().forEach(simulation -> {
            // Only update running simulations
            if (simulation.isRunning()) {
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
        });
    }

    /**
     * Sends simulation updates to the WebSocket channels
     */
    public void sendSimulationUpdate(Simulation simulation) {
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
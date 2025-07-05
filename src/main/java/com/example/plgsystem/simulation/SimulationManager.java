package com.example.plgsystem.simulation;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.orchest.DataReader;
import com.example.plgsystem.orchest.Event;
import com.example.plgsystem.orchest.EventType;
import com.example.plgsystem.orchest.Orchestrator;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages multiple simulation instances for concurrent use
 */
@Service
public class SimulationManager {
    private final Map<String, Simulation> simulations;
    private final Map<String, Orchestrator> orchestrators;
    private final Map<String, ScheduledFuture<?>> simulationTasks;
    private final Map<String, Integer> simulationSpeeds;
    
    private final DataReader dataReader;
    private final ScheduledExecutorService scheduler;
    
    /**
     * Default constructor for Spring dependency injection
     */
    public SimulationManager() {
        this.simulations = Collections.synchronizedMap(new HashMap<>());
        this.orchestrators = Collections.synchronizedMap(new HashMap<>());
        this.simulationTasks = Collections.synchronizedMap(new HashMap<>());
        this.simulationSpeeds = Collections.synchronizedMap(new HashMap<>());
        this.dataReader = new DataReader();
        this.scheduler = Executors.newScheduledThreadPool(10);
    }
    
    /**
     * Creates a new simulation with the given parameters
     * 
     * @param name The name of the simulation
     * @param description The description of the simulation
     * @param startDateTime The start date/time of the simulation
     * @return The created simulation
     */
    public Simulation createSimulation(String name, String description, LocalDateTime startDateTime) {
        // Create initial state with default values
        List<Vehicle> vehicles = dataReader.loadVehicles(null);
        
        // Create default depots
        Position centralPosition = new Position(12, 8); // Example position
        Depot mainDepot = new Depot("CENTRAL", centralPosition, 1000, true);
        mainDepot.refillGLP();
        
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("NORTH", new Position(45, 12), 160, false));
        auxDepots.add(new Depot("EAST", new Position(78, 45), 160, false));
        
        for (Depot depot : auxDepots) {
            depot.refillGLP();
        }
        
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startDateTime);
        
        // Create simulation
        Simulation simulation;
        if (name != null && description != null) {
            simulation = new Simulation(state, name, description);
        } else {
            simulation = new Simulation(state);
        }
        
        // Create orchestrator
        Orchestrator orchestrator = new Orchestrator(state);
        orchestrator.initialize();
        
        // Store simulation and orchestrator
        String simulationId = simulation.getId();
        simulations.put(simulationId, simulation);
        orchestrators.put(simulationId, orchestrator);
        simulationSpeeds.put(simulationId, 1); // Default speed
        
        return simulation;
    }
    
    /**
     * Gets a simulation by its ID
     * 
     * @param id The ID of the simulation
     * @return The simulation or empty if not found
     */
    public Optional<Simulation> getSimulation(String id) {
        return Optional.ofNullable(simulations.get(id));
    }
    
    /**
     * Gets all simulations
     * 
     * @return List of all simulations
     */
    public List<Simulation> getAllSimulations() {
        return new ArrayList<>(simulations.values());
    }
    
    /**
     * Deletes a simulation by its ID
     * 
     * @param id The ID of the simulation to delete
     * @return true if the simulation was found and deleted, false otherwise
     */
    public boolean deleteSimulation(String id) {
        stopSimulation(id); // Stop any running task
        
        Simulation removed = simulations.remove(id);
        orchestrators.remove(id);
        simulationSpeeds.remove(id);
        
        return removed != null;
    }
    
    /**
     * Starts or resumes a simulation
     * 
     * @param id The ID of the simulation
     * @return true if the simulation was started, false if not found
     */
    public boolean startSimulation(String id) {
        Optional<Simulation> simulationOpt = getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return false;
        }
        
        // Cancel any existing task
        stopSimulation(id);
        
        // Get orchestrator and simulation
        Orchestrator orchestrator = orchestrators.get(id);
        orchestrator.prepareSimulation();
        
        // Get speed factor (default 1)
        int speedFactor = simulationSpeeds.getOrDefault(id, 1);
        
        // Calculate delay based on speed factor (higher speed = lower delay)
        long delayMs = 1000 / speedFactor;
        
        // Schedule simulation task
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean continueSimulation = orchestrator.advanceTick();
                if (!continueSimulation) {
                    stopSimulation(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopSimulation(id);
            }
        }, 0, delayMs, TimeUnit.MILLISECONDS);
        
        simulationTasks.put(id, task);
        
        return true;
    }
    
    /**
     * Pauses a simulation
     * 
     * @param id The ID of the simulation
     * @return true if the simulation was paused, false if not found
     */
    public boolean pauseSimulation(String id) {
        return stopSimulation(id);
    }
    
    /**
     * Stops a simulation task
     * 
     * @param id The ID of the simulation
     * @return true if a task was found and stopped, false otherwise
     */
    private boolean stopSimulation(String id) {
        ScheduledFuture<?> task = simulationTasks.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a simulation is currently running
     * 
     * @param id The ID of the simulation
     * @return true if the simulation is running, false otherwise
     */
    public boolean isSimulationRunning(String id) {
        ScheduledFuture<?> task = simulationTasks.get(id);
        return task != null && !task.isCancelled() && !task.isDone();
    }
    
    /**
     * Sets the speed factor of a simulation
     * 
     * @param id The ID of the simulation
     * @param speedFactor The speed factor (1 = normal, 2 = 2x speed, etc.)
     * @return true if the simulation was found and the speed was set, false otherwise
     */
    public boolean setSimulationSpeed(String id, int speedFactor) {
        if (speedFactor <= 0) {
            return false;
        }
        
        Optional<Simulation> simulationOpt = getSimulation(id);
        if (simulationOpt.isEmpty()) {
            return false;
        }
        
        // Update speed factor
        simulationSpeeds.put(id, speedFactor);
        
        // Restart simulation if running to apply new speed
        if (isSimulationRunning(id)) {
            startSimulation(id);
        }
        
        return true;
    }
    
    /**
     * Simulates a vehicle breakdown in a simulation
     * 
     * @param simulationId The ID of the simulation
     * @param vehicleId The ID of the vehicle
     * @return true if the operation was successful, false otherwise
     */
    public boolean simulateVehicleBreakdown(String simulationId, String vehicleId) {
        Optional<Simulation> simulationOpt = getSimulation(simulationId);
        if (simulationOpt.isEmpty()) {
            return false;
        }
        
        Simulation simulation = simulationOpt.get();
        SimulationState state = simulation.getState();
        Vehicle vehicle = state.findVehicleById(vehicleId);
        
        if (vehicle == null) {
            return false;
        }
        
        // Create a breakdown event for immediate execution
        Event breakdownEvent = new Event(EventType.VEHICLE_BREAKDOWN, state.getCurrentTime());
        breakdownEvent.setEntityId(vehicleId);
        
        // Add event to orchestrator
        Orchestrator orchestrator = orchestrators.get(simulationId);
        orchestrator.addEvents(List.of(breakdownEvent));
        
        return true;
    }
    
    /**
     * Repairs a vehicle in a simulation
     * 
     * @param simulationId The ID of the simulation
     * @param vehicleId The ID of the vehicle
     * @return true if the operation was successful, false otherwise
     */
    public boolean repairVehicle(String simulationId, String vehicleId) {
        Optional<Simulation> simulationOpt = getSimulation(simulationId);
        if (simulationOpt.isEmpty()) {
            return false;
        }
        
        Simulation simulation = simulationOpt.get();
        SimulationState state = simulation.getState();
        Vehicle vehicle = state.findVehicleById(vehicleId);
        
        if (vehicle == null) {
            return false;
        }
        
        // Check if vehicle is actually broken
        if (vehicle.getStatus() != VehicleStatus.INCIDENT) {
            return false;
        }
        
        // Repair the vehicle
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        
        return true;
    }
    
    /**
     * Shutdown the simulation manager and all running simulations
     */
    public void shutdown() {
        for (String id : new ArrayList<>(simulationTasks.keySet())) {
            stopSimulation(id);
        }
        
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

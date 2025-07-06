package com.example.plgsystem.simulation;

import com.example.plgsystem.dto.SimulationReportDTO;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.orchest.AlgorithmConfig;
import com.example.plgsystem.orchest.DataReader;
import com.example.plgsystem.orchest.Event;
import com.example.plgsystem.orchest.EventType;
import com.example.plgsystem.orchest.Orchestrator;
import com.example.plgsystem.orchest.SimulationStats;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final Map<String, String> simulationTypes; // Stores simulation type (daily, weekly, collapse)
    private final Map<String, SimulationReportDTO> simulationReports; // Stores completed simulation reports
    
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
        this.simulationTypes = Collections.synchronizedMap(new HashMap<>());
        this.simulationReports = Collections.synchronizedMap(new HashMap<>());
        this.dataReader = new DataReader();
        this.scheduler = Executors.newScheduledThreadPool(10);
    }
    
    /**
     * Creates a new simulation with the given parameters
     * 
     * @param name The name of the simulation
     * @param description The description of the simulation
     * @param startDateTime The start date/time of the simulation
     * @param simulationType The type of simulation: "daily", "weekly", or "collapse"
     * @return The created simulation
     */
    public Simulation createSimulation(String name, String description, LocalDateTime startDateTime, String simulationType) {
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
        
        // For simulation modes (not daily operations), reset vehicles to main depot
        if (!"daily".equalsIgnoreCase(simulationType)) {
            resetVehiclesToMainDepot(vehicles, mainDepot.getPosition());
        }
        
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startDateTime);
        
        // Create simulation
        Simulation simulation;
        if (name != null && description != null) {
            simulation = new Simulation(state, name, description);
        } else {
            simulation = new Simulation(state);
        }
        
        // Create orchestrator with appropriate configuration based on simulation type
        Orchestrator orchestrator = new Orchestrator(state);
        AlgorithmConfig config;
        
        switch (simulationType.toLowerCase()) {
            case "daily":
                config = AlgorithmConfig.createOperationsMode();
                break;
            case "weekly":
                config = AlgorithmConfig.createWeeklySimulation();
                break;
            case "collapse":
                config = AlgorithmConfig.createCollapseSimulation();
                break;
            default:
                config = AlgorithmConfig.createDefault();
                break;
        }
        
        orchestrator.setAlgorithmConfig(config);
        orchestrator.initialize();
        
        // Store simulation, orchestrator, and simulation type
        String simulationId = simulation.getId();
        simulations.put(simulationId, simulation);
        orchestrators.put(simulationId, orchestrator);
        simulationSpeeds.put(simulationId, 1); // Default speed
        simulationTypes.put(simulationId, simulationType.toLowerCase());
        
        return simulation;
    }
    
    /**
     * Reset vehicles to available state at the main depot for simulation scenarios
     */
    private void resetVehiclesToMainDepot(List<Vehicle> vehicles, Position mainDepotPosition) {
        for (Vehicle vehicle : vehicles) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicle.setCurrentPosition(new Position(mainDepotPosition.getX(), mainDepotPosition.getY()));
            vehicle.refuel(); // Set fuel to maximum capacity
            vehicle.dispenseGlp(vehicle.getCurrentGlpM3()); // Empty GLP (dispense all current GLP)
        }
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
     * Gets the report for a completed simulation
     * 
     * @param id The ID of the simulation
     * @return The simulation report or empty if not found
     */
    public Optional<SimulationReportDTO> getSimulationReport(String id) {
        return Optional.ofNullable(simulationReports.get(id));
    }
    
    /**
     * Gets all simulation reports
     * 
     * @return List of all simulation reports
     */
    public List<SimulationReportDTO> getAllSimulationReports() {
        return new ArrayList<>(simulationReports.values());
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
        simulationTypes.remove(id);
        
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
        
        // Start tracking simulation stats
        orchestrator.getStats().startSimulation(simulationOpt.get().getState().getCurrentTime());
        
        // Get speed factor (default 1)
        int speedFactor = simulationSpeeds.getOrDefault(id, 1);
        
        // Get simulation type
        String simulationType = simulationTypes.getOrDefault(id, "weekly");
        
        // Calculate delay based on speed factor and simulation type
        long delayMs;
        if ("daily".equalsIgnoreCase(simulationType)) {
            // Operations mode - 1-second ticks
            delayMs = 1000 / speedFactor; // Base delay is 1000ms (1 second)
        } else {
            // Simulation modes - 1-minute ticks, but run faster
            delayMs = 50 / speedFactor; // Much faster simulation for analysis
        }
        
        // Schedule simulation task
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean continueSimulation = orchestrator.advanceTick();
                if (!continueSimulation) {
                    // Simulation has ended - generate final report
                    Simulation simulation = simulationOpt.get();
                    
                    // Record end time in stats
                    orchestrator.getStats().endSimulation(simulation.getState().getCurrentTime());
                    
                    // Generate report
                    SimulationReportDTO report = generateSimulationReport(id, orchestrator.getStats());
                    
                    // Store report
                    simulationReports.put(id, report);
                    
                    // Stop the simulation
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
     * Generates a simulation report from the current stats
     * 
     * @param simulationId The ID of the simulation
     * @param stats The simulation statistics
     * @return The generated simulation report
     */
    private SimulationReportDTO generateSimulationReport(String simulationId, SimulationStats stats) {
        Optional<Simulation> simulationOpt = getSimulation(simulationId);
        if (simulationOpt.isEmpty()) {
            return null;
        }
        
        Simulation simulation = simulationOpt.get();
        String simulationType = simulationTypes.get(simulationId);
        
        // Record real execution time
        long endTimeMs = System.currentTimeMillis();
        stats.setRealExecutionTimeMillis(endTimeMs - simulation.getCreatedAt().toLocalTime().toNanoOfDay() / 1_000_000);
        
        return SimulationReportDTO.fromSimulationStats(
            simulationId,
            simulationType,
            simulation.getName(),
            stats
        );
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
    
    /**
     * Configura una simulación existente con datos específicos
     * 
     * @param simulationId El ID de la simulación a configurar
     * @param dataSource La ruta o identificador de la fuente de datos (archivos)
     * @param durationDays La duración máxima de la simulación en días (0 para ilimitado)
     * @return true si la configuración fue exitosa, false si no
     */
    public boolean configureSimulation(String simulationId, String dataSource, int durationDays) {
        Optional<Simulation> simulationOpt = getSimulation(simulationId);
        if (simulationOpt.isEmpty()) {
            return false;
        }
        
        Simulation simulation = simulationOpt.get();
        Orchestrator orchestrator = orchestrators.get(simulationId);
        
        if (orchestrator == null) {
            return false;
        }
        
        // Get the simulation type
        String simulationType = simulationTypes.get(simulationId);
        
        // Configure algorithm parameters
        AlgorithmConfig config;
        switch (simulationType) {
            case "daily":
                config = AlgorithmConfig.createOperationsMode();
                break;
            case "weekly":
                config = AlgorithmConfig.createWeeklySimulation();
                durationDays = 7; // Force exactly 7 days for weekly simulation
                break;
            case "collapse":
                config = AlgorithmConfig.createCollapseSimulation();
                break;
            default:
                config = AlgorithmConfig.createDefault();
                break;
        }
        
        // Set custom simulation duration if specified
        if (durationDays > 0) {
            config.setSimulationMaxDays(durationDays);
        }
        
        // Update the orchestrator with new configuration
        orchestrator.setAlgorithmConfig(config);
        
        if (durationDays > 0) {
            // Set simulation end time explicitly if we have a duration
            LocalDateTime endTime = simulation.getState().getCurrentTime().plusDays(durationDays);
            Event endEvent = new Event(EventType.SIMULATION_END, endTime);
            orchestrator.addEvents(List.of(endEvent));
        }
        
        // Load data from files if dataSource is provided
        if (dataSource != null && !dataSource.isEmpty()) {
            SimulationState state = simulation.getState();
            LocalDateTime startTime = state.getCurrentTime();
            
            try {
                // Load orders from files
                List<Order> orders = dataReader.loadOrders(
                    dataSource + "/pedidos", 
                    startTime, 
                    durationDays * 24, // Convert days to hours
                    0 // No limit on number of orders
                );
                state.addOrders(orders);
                
                // Load blockages from files
                List<Blockage> blockages = dataReader.loadBlockages(
                    dataSource + "/bloqueos", 
                    startTime, 
                    durationDays * 24, // Convert days to hours
                    0 // No limit on number of blockages
                );
                state.addBlockages(blockages);
                
                // Load maintenance tasks from files
                List<Maintenance> maintenanceTasks = dataReader.loadMaintenanceSchedule(
                    dataSource + "/mantpreventivo.txt",
                    startTime,
                    durationDays,
                    0 // No limit on number of maintenance tasks
                );
                state.addMaintenanceTasks(maintenanceTasks);
                
                // Create events for orders and blockages
                List<Event> events = new ArrayList<>();
                
                // Order arrival events
                for (Order order : orders) {
                    Event event = new Event(EventType.ORDER_ARRIVAL, order.getArriveTime());
                    event.setEntityId(order.getId());
                    event.setData(order);
                    events.add(event);
                }
                
                // Blockage start/end events
                for (Blockage blockage : blockages) {
                    Event startEvent = new Event(EventType.BLOCKAGE_START, blockage.getStartTime());
                    startEvent.setEntityId("blockage-" + blockage.hashCode());
                    startEvent.setData(blockage);
                    events.add(startEvent);
                    
                    Event endEvent = new Event(EventType.BLOCKAGE_END, blockage.getEndTime());
                    endEvent.setEntityId("blockage-" + blockage.hashCode());
                    events.add(endEvent);
                }
                
                // Maintenance events
                for (Maintenance task : maintenanceTasks) {
                    LocalDateTime taskStartTime = LocalDateTime.of(task.getDate(), LocalTime.of(0, 0));
                    LocalDateTime endTime = taskStartTime.plusHours(24); // Maintenance lasts a day
                    
                    Event startEvent = new Event(EventType.MAINTENANCE_START, taskStartTime);
                    startEvent.setEntityId(task.getVehicleId());
                    startEvent.setData(task);
                    events.add(startEvent);
                    
                    Event endEvent = new Event(EventType.MAINTENANCE_END, endTime);
                    endEvent.setEntityId(task.getVehicleId());
                    events.add(endEvent);
                }
                
                // Add all events to orchestrator
                orchestrator.addEvents(events);
                
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        return true;
    }
}

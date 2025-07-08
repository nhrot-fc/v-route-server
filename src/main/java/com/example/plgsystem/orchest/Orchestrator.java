package com.example.plgsystem.orchest;

import com.example.plgsystem.assignation.DeliveryPart;
import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.operation.VehiclePlanCreator;
import com.example.plgsystem.simulation.SimulationState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class Orchestrator {
    private static final Logger logger = Logger.getLogger(Orchestrator.class.getName());

    private final SimulationState environment;
    private final Map<Vehicle, VehiclePlan> vehiclePlans;
    private final List<Event> eventQueue;

    private LocalDateTime simulationTime;
    private boolean simulationRunning;
    private boolean needsReplanning;

    // Tick counter for replanning
    private int tickCounter;
    private int ticksPerReplan;

    public Orchestrator(SimulationState environment) {
        this.environment = environment;
        this.vehiclePlans = new HashMap<>();
        this.simulationTime = environment.getCurrentTime();
        this.simulationRunning = false;
        this.eventQueue = new ArrayList<>();
        this.needsReplanning = false;
        this.tickCounter = 0;
        this.ticksPerReplan = 60;
    }

    public void addEvents(List<Event> events) {
        this.eventQueue.addAll(events);
        this.eventQueue.sort(Comparator.comparing(Event::getTime));
    }

    public void startSimulation() {
        if (simulationRunning) {
            logger.warning("Simulation is already running.");
            return;
        }
        simulationRunning = true;
        logger.info("Starting simulation at " + simulationTime);
    }

    public void stopSimulation() {
        if (!simulationRunning) {
            logger.warning("Simulation is not running.");
            return;
        }
        simulationRunning = false;
        logger.info("Stopping simulation at " + simulationTime);
    }

    /**
     * Main method that advances the simulation by one tick
     */
    public boolean advanceTick() {
        // Update environment state and process events
        updateEnvironment();

        // Execute current vehicle plans
        executeVehiclePlans();

        // Increment tick counter
        tickCounter++;

        // Log environment state periodically
        if (tickCounter % ticksPerReplan == 0) {
            logger.info(environment.toString());
        }

        // Check if replanning is needed
        boolean tickBasedReplanning = tickCounter >= ticksPerReplan;

        // Perform replanning if needed and there are vehicles available
        if ((needsReplanning || tickBasedReplanning)) {
            replanVehicles();
            needsReplanning = false;
            tickCounter = 0; // Reset tick counter after replanning
        }

        // Advance simulation time
        advanceSimulation();

        return simulationRunning;
    }

    /**
     * Updates the environment state and processes pending events
     */
    private void updateEnvironment() {
        // Update environment time
        environment.setCurrentTime(simulationTime);

        // Process all events that have occurred up to the current time
        processEvents();
    }

    private void processEvents() {
        while (!eventQueue.isEmpty() && eventQueue.getFirst().getTime().isBefore(simulationTime)) {
            Event event = eventQueue.removeFirst();
            processEvent(event);
        }
    }

    private void processEvent(Event event) {
        logger.info("Processing event: " + event);

        switch (event.getType()) {
            case ORDER_ARRIVAL:
                if (event.getEntityId() != null && event.getData() != null) {
                    Order order = (Order) event.getData();
                    environment.addOrder(order);
                    logger.info("Added new order to environment: " + order.getId());
                    needsReplanning = false;
                }
                break;

            case BLOCKAGE_START:
                if (event.getData() != null) {
                    Blockage blockage = (Blockage) event.getData();
                    environment.addBlockage(blockage);
                    logger.info("Blockage started: " + blockage);
                    needsReplanning = false;
                }
                break;

            case BLOCKAGE_END:
                // The environment should handle removing expired blockages
                logger.info("Blockage ended with ID: " + event.getEntityId());
                needsReplanning = false;
                break;

            case VEHICLE_BREAKDOWN:
                if (event.getEntityId() != null) {
                    String vehicleId = event.getEntityId();
                    // Find the vehicle and update its status
                    for (Vehicle vehicle : environment.getVehicles()) {
                        if (vehicle.getId().equals(vehicleId)) {
                            vehicle.setStatus(VehicleStatus.INCIDENT);
                            logger.info("Vehicle breakdown: " + vehicleId);

                            // Remove any plans for this vehicle
                            vehiclePlans.remove(vehicle);

                            needsReplanning = true;
                            break;
                        }
                    }
                }
                break;

            case MAINTENANCE_START:
                if (event.getEntityId() != null && event.getData() != null) {
                    Maintenance task = (Maintenance) event.getData();
                    environment.addMaintenance(task);

                    // Update vehicle status to MAINTENANCE
                    for (Vehicle vehicle : environment.getVehicles()) {
                        if (vehicle.getId().equals(event.getEntityId())) {
                            vehicle.setStatus(VehicleStatus.MAINTENANCE);

                            // Remove any plans for this vehicle
                            vehiclePlans.remove(vehicle);
                            break;
                        }
                    }

                    logger.info("Maintenance started for vehicle: " + event.getEntityId());
                    needsReplanning = true;
                }
                break;

            case MAINTENANCE_END:
                // Find the maintenance task and mark it as completed
                String vehicleId = event.getEntityId();
                if (vehicleId != null) {
                    for (Vehicle vehicle : environment.getVehicles()) {
                        if (vehicle.getId().equals(vehicleId)) {
                            vehicle.setStatus(VehicleStatus.AVAILABLE);
                            logger.info("Maintenance ended for vehicle: " + vehicleId);
                            needsReplanning = true;
                            break;
                        }
                    }
                }
                break;

            case GLP_DEPOT_REFILL:
                for (Depot depot : environment.getAuxDepots()) {
                    depot.refill();
                    logger.info("GLP depot refilled: " + depot.getId());
                }

                break;

            case SIMULATION_END:
                logger.info("Simulation end event received");
                simulationRunning = false;
                break;

            default:
                logger.warning("Unknown event type: " + event.getType());
                break;
        }
    }

    private void executeVehiclePlans() {
        for (Map.Entry<Vehicle, VehiclePlan> entry : vehiclePlans.entrySet()) {
            Vehicle vehicle = entry.getKey();
            VehiclePlan plan = entry.getValue();

            if (vehicle.getStatus() == VehicleStatus.INCIDENT || plan == null) {
                logger.fine("Skipping vehicle " + vehicle.getId() + " (unavailable or no plan)");
                continue;
            }

            for (Action action : plan.getActions()) {
                if (action.getExpectedStartTime().isBefore(simulationTime)) {
                    action.execute(vehicle, environment, simulationTime);
                    logger.fine("Executed action: " + action + " for vehicle: " + vehicle.getId());
                } else {
                    logger.fine("Skipping action: " + action + " for vehicle: " + vehicle.getId()
                            + " as it is scheduled for future time.");
                }
            }
        }
    }

    /**
     * Replans vehicle assignments based on current environment state
     */
    private void replanVehicles() {
        logger.info(String.format("Replanning vehicles at [%s] | with %d/%d ticks per replan | needsReplanning: %b",
                simulationTime, tickCounter, ticksPerReplan, needsReplanning));

        // Check if there are any pending orders and available vehicles
        List<Order> pendingOrders = environment.getOrders();
        List<Vehicle> availableVehicles = environment.getVehicles();

        if (pendingOrders.isEmpty()) {
            logger.info("No pending orders to deliver. Skipping replanning.");
            return;
        }

        if (availableVehicles.isEmpty()) {
            logger.info("No available vehicles for delivery. Skipping replanning.");
            return;
        }

        // Log statistics before replanning for comparison
        int previousPlanCount = vehiclePlans.size();
        int pendingOrdersCount = pendingOrders.size();

        // Run the assignation algorithm
        runAssignation();

        // Log the results
        logger.info(String.format("Replanning completed: %d plans (previously %d) for %d pending orders",
                vehiclePlans.size(), previousPlanCount, pendingOrdersCount));
    }

    /**
     * Runs the metaheuristic assignation algorithm and creates vehicle plans
     */
    private void runAssignation() {
        // Check if there are any available vehicles
        List<Vehicle> availableVehicles = environment.getVehicles();
        if (availableVehicles.isEmpty()) {
            logger.info("No available vehicles for assignation. Skipping assignation.");
            return;
        }

        // Check if there are any pending orders
        List<Order> pendingOrders = environment.getOrders();

        if (pendingOrders.isEmpty()) {
            logger.info("No pending orders to assign. Creating default plans to return to main depot.");

            // Create default plans for all available vehicles to return to the main depot
            for (Vehicle vehicle : availableVehicles) {
                VehiclePlan defaultPlan = VehiclePlanCreator.createPlanToMainDepot(environment, vehicle);
                if (defaultPlan != null) {
                    vehiclePlans.put(vehicle, defaultPlan);
                    logger.info("Created default plan for vehicle " + vehicle.getId() + " to return to main depot");
                    logger.fine(defaultPlan.toString());
                }
            }

            logger.info("Created " + vehiclePlans.size() + " default plans for vehicles to return to main depot");
            return;
        }

        // Proceed with assignation when we have both orders and vehicles
        Solution solution = MetaheuristicSolver.solve(environment);

        // Create plans for vehicles with delivery instructions
        for (Map.Entry<String, List<DeliveryPart>> entry : solution.getVehicleOrderAssignments().entrySet()) {
            String vehicleId = entry.getKey();
            Vehicle vehicle = environment.getVehicleById(vehicleId);
            List<DeliveryPart> instructions = entry.getValue();

            // Only create plans for vehicles with actual instructions
            if (!instructions.isEmpty()) {
                VehiclePlan plan = VehiclePlanCreator.createPlan(environment, vehicle, instructions);
                if (plan != null) {
                    vehiclePlans.put(vehicle, plan);
                    logger.info(plan.toString());
                } else {
                    logger.warning("Failed to create plan for vehicle: " + vehicle.getId());
                }
            }
        }

        // Create default plans for vehicles without assignments
        for (Vehicle vehicle : availableVehicles) {
            // Skip unavailable vehicles
            if (vehicle.getStatus() == VehicleStatus.INCIDENT) {
                continue;
            }

            // This vehicle has no assigned deliveries, create a plan to return to the main
            // depot
            VehiclePlan defaultPlan = VehiclePlanCreator.createPlanToMainDepot(environment, vehicle);
            if (defaultPlan != null) {
                vehiclePlans.put(vehicle, defaultPlan);
                logger.info(
                        "Created default plan for unassigned vehicle " + vehicle.getId() + " to return to main depot");
                logger.fine(defaultPlan.toString());
            } else {
                logger.warning("Failed to create default plan for vehicle: " + vehicle.getId());
            }
        }

        logger.info("Assignation completed with " + vehiclePlans.size() + " vehicle plans created.");
    }

    /**
     * Advances the simulation time by the configured step amount
     */
    private void advanceSimulation() {
        simulationTime = simulationTime.plusMinutes(1);
        environment.advanceTime(Duration.ofMinutes(1));
        logger.fine("Advanced simulation to " + simulationTime);
    }

    public Map<Vehicle, VehiclePlan> getVehiclePlans() {
        return Collections.unmodifiableMap(vehiclePlans);
    }

    public void initialize() {
        // Add a simulation end event based on config.getSimulationMaxDays()
        LocalDateTime endTime = this.simulationTime.plusDays(1);
        Event endEvent = new Event(EventType.SIMULATION_END, endTime);
        this.eventQueue.add(endEvent);
        this.eventQueue.sort(Comparator.comparing(Event::getTime));

        // Make sure environment time is synced with simulation time
        environment.setCurrentTime(this.simulationTime);

        logger.info("Orchestrator initialized with time: " + this.simulationTime);
        logger.info("Simulation end scheduled for: " + endTime);
    }

    /**
     * Replaces the startSimulation method since we're advancing ticks from the UI
     * now.
     * Just makes sure the simulation is ready to run.
     */
    public void prepareSimulation() {
        if (simulationRunning) {
            logger.warning("Simulation is already running.");
            return;
        }
        simulationRunning = true;
        logger.info("Simulation prepared to start at " + simulationTime);
        environment.setCurrentTime(simulationTime);
    }

    /**
     * Sets the number of ticks between replanning operations
     * 
     * @param ticks Number of ticks between replans
     */
    public void setTicksPerReplan(int ticks) {
        if (ticks <= 0) {
            logger.warning("Invalid ticks per replan: " + ticks + ". Using default value.");
            return;
        }
        this.ticksPerReplan = ticks;
        logger.info("Replanning frequency set to: " + ticks + " ticks");
    }
}

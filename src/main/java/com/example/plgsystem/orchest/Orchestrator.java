package com.example.plgsystem.orchest;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionType;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.simulation.SimulationState;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.operation.VehiclePlanCreator;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import com.example.plgsystem.model.Position;

@Getter
public class Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

    private final SimulationState environment;
    private final PriorityQueue<Event> eventQueue;
    private final int minutesForReplan;
    private final DataLoader dataLoader;

    private LocalDateTime simulationTime;
    private boolean needsReplanning;
    private Duration tickDuration;
    private LocalDateTime lastReplanningTime;
    private LocalDateTime lastEventsCheckTime;
    private int eventsCheckFrequency;
    private int ticksSinceLastCheck;

    private boolean isReplanning;
    private Thread replanningThread;
    private Runnable onReplanningComplete;

    public Orchestrator(SimulationState environment, Duration tickDuration, int minutesForReplan,
            DataLoader dataLoader) {
        this.environment = environment;
        this.simulationTime = environment.getCurrentTime();
        this.tickDuration = tickDuration;
        this.eventQueue = new PriorityQueue<>(Comparator.comparing(Event::getTime));
        this.needsReplanning = false;
        this.lastReplanningTime = simulationTime;
        this.minutesForReplan = minutesForReplan;
        this.dataLoader = dataLoader;
        this.replanningThread = null;

        // Initialize event checking variables
        this.lastEventsCheckTime = simulationTime;
        this.eventsCheckFrequency = 20;
        this.ticksSinceLastCheck = 0;

        // Schedule the first NEW_DAY_BEGIN event
        LocalDateTime nextNewDay = LocalDateTime.of(
                simulationTime.toLocalDate().plusDays(1),
                LocalTime.of(0, 0));
        this.eventQueue.add(new Event(EventType.NEW_DAY_BEGIN, nextNewDay, null, null));
    }

    public Map<String, VehiclePlan> getVehiclePlans() {
        return environment.getCurrentVehiclePlans();
    }

    public void addEvents(List<Event> events) {
        this.eventQueue.addAll(events);
    }

    public void addEvent(Event event) {
        this.eventQueue.add(event);
    }

    /**
     * Sets a callback to be executed when replanning completes.
     * This is useful for notifying other components about the completion of
     * replanning.
     * 
     * @param callback A Runnable to be executed when replanning completes
     */
    public void setOnReplanningComplete(Runnable callback) {
        this.onReplanningComplete = callback;
    }

    /**
     * Gets the frequency at which the system checks for new events.
     * 
     * @return The number of ticks between event checks
     */
    public int getEventsCheckFrequency() {
        return eventsCheckFrequency;
    }

    /**
     * Sets the frequency at which the system checks for new events.
     * 
     * @param frequency The number of ticks between event checks
     */
    public void setEventsCheckFrequency(int frequency) {
        if (frequency < 1) {
            throw new IllegalArgumentException("Event check frequency must be at least 1");
        }
        this.eventsCheckFrequency = frequency;
    }

    public void advanceTick() {
        if (isReplanning) {
            return;
        }

        LocalDateTime nextTick = simulationTime.plus(tickDuration);

        // Process events scheduled before the next tick
        while (!eventQueue.isEmpty() && eventQueue.peek().getTime().isBefore(nextTick)) {
            Event event = eventQueue.poll();
            processEvent(event);
        }

        // Check for new events periodically
        ticksSinceLastCheck++;
        if (ticksSinceLastCheck >= eventsCheckFrequency) {
            checkAndLoadNewEvents();
            ticksSinceLastCheck = 0;
        }

        executeVehiclePlans(nextTick);

        // Assign return-to-depot plans for available vehicles without any plan
        createReturnToDepotPlansForIdleVehicles();

        int minutesSinceLastReplanning = (int) Duration.between(lastReplanningTime, nextTick).toMinutes();
        if ((needsReplanning || (minutesSinceLastReplanning >= minutesForReplan))) {
            replan();
            needsReplanning = false;
            lastReplanningTime = nextTick;
        }

        environment.setCurrentTime(nextTick);
        simulationTime = nextTick;
    }

    public void replan() {
        // If already replanning, don't start another thread
        if (isReplanning) {
            logger.info("Replanning already in progress, skipping new replan request");
            return;
        }

        logger.info("Starting replanning at {}", simulationTime);
        isReplanning = true;

        // Cancel any existing replanning thread
        if (replanningThread != null && replanningThread.isAlive()) {
            replanningThread.interrupt();
        }

        // Create a snapshot of the environment for replanning
        final SimulationState environmentSnapshot = environment.createSnapshot();

        // Create a new thread for replanning
        replanningThread = new Thread(() -> {
            try {
                logger.info("Replanning thread started");
                Solution solution = MetaheuristicSolver.solve(environmentSnapshot);

                // Apply the solution to the actual environment
                synchronized (environment) {
                    // Clear existing plans for all vehicles
                    environment.getCurrentVehiclePlans().clear();

                    for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
                        String vehicleId = entry.getKey();
                        Route route = entry.getValue();
                        Vehicle vehicle = environment.getVehicleById(vehicleId);

                        if (vehicle == null) {
                            logger.error("Vehicle not found for ID: {}", vehicleId);
                            continue;
                        }

                        try {
                            VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, environment);
                            environment.getCurrentVehiclePlans().put(vehicleId, plan);
                        } catch (Exception e) {
                            logger.error("Error creating plan for vehicle {}: {}", vehicleId, e.getMessage());
                        }
                    }

                    // Create return-to-depot plans for any available vehicles without assigned
                    // routes
                    createReturnToDepotPlansForIdleVehicles();

                    logger.info("Replanning completed. Created {} new vehicle plans",
                            environment.getCurrentVehiclePlans().size());
                }

                // Notify listeners that replanning is complete
                if (onReplanningComplete != null) {
                    onReplanningComplete.run();
                }
            } catch (Exception e) {
                logger.error("Error during replanning: {}", e.getMessage(), e);
            } finally {
                isReplanning = false;
            }
        });

        replanningThread.setName("ReplanningThread-" + simulationTime);
        replanningThread.start();
    }

    private void checkAndLoadNewEvents() {
        logger.info("Checking for new events from data loader");
        Set<String> currentOrders = new HashSet<>();
        Set<String> currentBlockages = new HashSet<>();

        for (Order order : environment.getOrders()) {
            currentOrders.add(order.getId());
        }

        for (Blockage blockage : environment.getBlockages()) {
            String blockageCustomId = String.format("%03d-%03d-%03d-%03d-%s-%s",
                    (int) blockage.getLines().get(0).getX(), (int) blockage.getLines().get(0).getY(),
                    (int) blockage.getLines().get(1).getX(), (int) blockage.getLines().get(1).getY(),
                    blockage.getStartTime(), blockage.getEndTime());
            currentBlockages.add(blockageCustomId);
        }

        List<Event> newOrderEvents = dataLoader.loadOrdersForDate(simulationTime.toLocalDate());
        List<Event> newBlockageEvents = dataLoader.loadBlockagesForDate(simulationTime.toLocalDate());

        List<Event> filteredEvents = new ArrayList<>();
        for (Event event : newOrderEvents) {
            if (event.getType() == EventType.ORDER_ARRIVAL) {
                Order order = (Order) event.getData();

                if (!currentOrders.contains(order.getId())) {
                    filteredEvents.add(event);
                }
            }
        }

        for (Event event : newBlockageEvents) {
            if (event.getType() == EventType.BLOCKAGE_START) {
                Blockage blockage = (Blockage) event.getData();

                String blockageCustomId = String.format("%03d-%03d-%03d-%03d-%s-%s",
                        (int) blockage.getLines().get(0).getX(), (int) blockage.getLines().get(0).getY(),
                        (int) blockage.getLines().get(1).getX(), (int) blockage.getLines().get(1).getY(),
                        blockage.getStartTime(), blockage.getEndTime());
                if (!currentBlockages.contains(blockageCustomId)) {
                    filteredEvents.add(event);
                }
            }
        }

        addEvents(filteredEvents);
        lastEventsCheckTime = simulationTime;
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
                            environment.getCurrentVehiclePlans().remove(vehicle.getId());

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
                            environment.getCurrentVehiclePlans().remove(vehicle.getId());
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

            case NEW_DAY_BEGIN:
                // Handle start of a new day
                LocalDate today = simulationTime.toLocalDate();
                logger.info("New day begins: " + today);

                // Refill all auxiliary depots (previously handled by GLP_DEPOT_REFILL)
                for (Depot depot : environment.getAuxDepots()) {
                    depot.refill();
                    logger.info("GLP depot refilled: " + depot.getId());
                }

                // Schedule the next day event
                LocalDateTime nextDay = LocalDateTime.of(
                        today.plusDays(1),
                        LocalTime.of(0, 0));
                addEvent(new Event(EventType.NEW_DAY_BEGIN, nextDay, null, null));
                logger.info("Scheduled next NEW_DAY_BEGIN event for " + nextDay);
                break;

            default:
                logger.warn("Unknown event type: " + event.getType());
                break;
        }
    }

    private void executeVehiclePlans(LocalDateTime nextTick) {
        for (Map.Entry<String, VehiclePlan> entry : environment.getCurrentVehiclePlans().entrySet()) {
            String vehicleId = entry.getKey();
            VehiclePlan plan = entry.getValue();
            Vehicle vehicle = environment.getVehicleById(vehicleId);

            if (vehicle.getStatus() == VehicleStatus.INCIDENT || plan == null) {
                logger.debug("Skipping vehicle " + vehicle.getId() + " (unavailable or no plan)");
                continue;
            }

            Action action = plan.getCurrentAction();
            if (action != null && action.getStartTime().isBefore(nextTick)) {
                executeAction(action, vehicle, environment, nextTick);
                logger.debug("Executed action: " + action + " for vehicle: " + vehicle.getId());
            } else {
                logger.debug("Skipping action for vehicle: " + vehicle.getId()
                        + " as it is scheduled for future time or no current action.");
            }
        }
    }

    private void executeAction(Action action, Vehicle vehicle, SimulationState environment, LocalDateTime nextTick) {
        if (action.getCurrentProgress() >= 1.0) {
            return;
        }

        double previousProgress = action.getCurrentProgress();
        LocalDateTime actionStart = action.getStartTime();
        LocalDateTime actionEnd = action.getEndTime();

        // Apply immediate effects if this is the first time we're processing this
        // action
        if (previousProgress == 0.0 && !action.isEffectApplied()) {
            applyImmediateEffects(action, vehicle, environment);
        }

        // Calculate progress based on time
        long totalDurationSeconds = Duration.between(actionStart, actionEnd).toSeconds();
        if (totalDurationSeconds <= 0) {
            action.setCurrentProgress(1.0);
            completeAction(action, vehicle, environment);
            return;
        }

        LocalDateTime effectiveTime = nextTick.isBefore(actionEnd) ? nextTick : actionEnd;
        long elapsedSeconds = Duration.between(actionStart, effectiveTime).toSeconds();
        double progress = Math.min(1.0, (double) elapsedSeconds / totalDurationSeconds);
        action.setCurrentProgress(progress);

        // Apply gradual effects (only for DRIVE action)
        if (action.getType() == ActionType.DRIVE) {
            applyGradualEffects(action, vehicle, environment, progress, previousProgress);
        }

        // If action is complete, move to next action
        if (progress >= 1.0) {
            completeAction(action, vehicle, environment);
            VehiclePlan plan = environment.getCurrentVehiclePlans().get(vehicle.getId());
            if (plan != null) {
                plan.advanceAction();
            }
        }
    }

    private void applyImmediateEffects(Action action, Vehicle vehicle, SimulationState environment) {
        switch (action.getType()) {
            case REFUEL:
                vehicle.refuel();
                logger.info("Vehicle {} refueled to capacity from depot {}", vehicle.getId(), action.getDepotId());
                action.setEffectApplied(true);
                break;

            case RELOAD:
                vehicle.refill(action.getGlpLoaded());
                logger.info("Vehicle {} loaded {} m³ of GLP from depot {}", vehicle.getId(), action.getGlpLoaded(),
                        action.getDepotId());

                // Update depot GLP levels
                Depot depot = environment.getDepotById(action.getDepotId());
                if (depot != null) {
                    depot.serve(action.getGlpLoaded());
                }
                action.setEffectApplied(true);
                break;

            case SERVE:
                vehicle.dispense(action.getGlpDelivered());
                logger.info("Vehicle {} delivered {} m³ of GLP to order {}", vehicle.getId(), action.getGlpDelivered(),
                        action.getOrderId());

                // Find and update order status
                Order order = environment.getOrderById(action.getOrderId());
                if (order != null) {
                    order.recordDelivery(action.getGlpDelivered(), vehicle, action.getStartTime());
                }
                action.setEffectApplied(true);
                break;

            case MAINTENANCE:
            case WAIT:
                // Update position immediately for MAINTENANCE and WAIT actions
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    vehicle.setCurrentPosition(action.getPath().get(0));
                }
                action.setEffectApplied(true);
                break;

            case DRIVE:
                // DRIVE has gradual effects, no immediate effects to apply here
                // Just update the initial position
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    vehicle.setCurrentPosition(action.getPath().get(0));
                }
                break;

            default:
                logger.warn("Unknown action type: {}", action.getType());
                break;
        }
    }

    private void applyGradualEffects(Action action, Vehicle vehicle, SimulationState environment,
            double currentProgress, double previousProgress) {
        if (previousProgress >= currentProgress) {
            return; // No additional effects to apply
        }

        double effectMultiplier = currentProgress - previousProgress;

        switch (action.getType()) {
            case DRIVE:
                // Update vehicle position based on path
                updateVehiclePosition(vehicle, action, currentProgress);

                // Apply fuel consumption proportional to progress
                double fuelToConsume = action.getFuelConsumedGal() * effectMultiplier;
                vehicle.consumeFuel(fuelToConsume);
                break;

            default:
                // Other action types don't have gradual effects
                break;
        }
    }

    private void completeAction(Action action, Vehicle vehicle, SimulationState environment) {
        // Ensure final position is set correctly for all action types
        if (action.getPath() != null && !action.getPath().isEmpty()) {
            vehicle.setCurrentPosition(action.getPath().get(action.getPath().size() - 1));
        }

        // For DRIVE, ensure full fuel consumption is applied
        if (action.getType() == ActionType.DRIVE && !action.isEffectApplied()) {
            vehicle.consumeFuel(action.getFuelConsumedGal());
            action.setEffectApplied(true);
        }
    }

    private void updateVehiclePosition(Vehicle vehicle, Action action, double progress) {
        List<Position> path = action.getPath();
        if (path == null || path.isEmpty()) {
            return;
        }

        if (path.size() == 1) {
            vehicle.setCurrentPosition(path.get(0));
            return;
        }

        if (progress <= 0) {
            vehicle.setCurrentPosition(path.get(0));
        } else if (progress >= 1.0) {
            vehicle.setCurrentPosition(path.get(path.size() - 1));
        } else {
            double segmentLength = 1.0 / (path.size() - 1);
            int segmentIndex = (int) Math.min(path.size() - 2, Math.floor(progress / segmentLength));
            double segmentProgress = (progress - segmentIndex * segmentLength) / segmentLength;

            Position start = path.get(segmentIndex);
            Position end = path.get(segmentIndex + 1);

            double newX = start.getX() + segmentProgress * (end.getX() - start.getX());
            double newY = start.getY() + segmentProgress * (end.getY() - start.getY());

            vehicle.setCurrentPosition(new Position(newX, newY));
        }
    }

    /**
     * Creates a plan for available vehicles without an assigned plan to return to
     * the main depot.
     * This ensures that idle vehicles are productive and return to the main depot
     * for refueling and refilling.
     */
    private void createReturnToDepotPlansForIdleVehicles() {
        // logger.info("Checking for available vehicles without plans...");
        int plansCreated = 0;

        for (Vehicle vehicle : environment.getVehicles()) {
            // Only consider vehicles that are available and don't already have a plan
            if (vehicle.getStatus() == VehicleStatus.AVAILABLE &&
                    !environment.getCurrentVehiclePlans().containsKey(vehicle.getId())) {

                // Don't create plans for vehicles already at the main depot
                Depot mainDepot = environment.getMainDepot();
                if (mainDepot != null && vehicle.getCurrentPosition().equals(mainDepot.getPosition())) {
                    logger.debug("Vehicle {} is already at the main depot, no plan needed", vehicle.getId());
                    continue;
                }

                try {
                    VehiclePlan depotPlan = VehiclePlanCreator.createPlanToMainDepot(vehicle, environment);

                    if (depotPlan != null) {
                        environment.getCurrentVehiclePlans().put(vehicle.getId(), depotPlan);
                        plansCreated++;
                        logger.info("Created return-to-depot plan for idle vehicle: {}", vehicle.getId());
                    } else {
                        logger.warn("Failed to create return-to-depot plan for vehicle: {}", vehicle.getId());
                    }
                } catch (Exception e) {
                    logger.error("Error creating return-to-depot plan for vehicle {}: {}",
                            vehicle.getId(), e.getMessage());
                }
            }
        }

        if (plansCreated > 0) {
            logger.info("Created {} return-to-depot plans for idle vehicles", plansCreated);
        }
    }
}

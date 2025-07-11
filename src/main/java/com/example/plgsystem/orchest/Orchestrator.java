package com.example.plgsystem.orchest;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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

    public void advanceTick() {
        LocalDateTime nextTick = simulationTime.plus(tickDuration);
        while (!eventQueue.isEmpty() && eventQueue.peek().getTime().isBefore(nextTick)) {
            Event event = eventQueue.poll();
            processEvent(event);
        }

        executeVehiclePlans(nextTick);
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
        logger.info("Starting replanning at {}", simulationTime);

        Solution solution = MetaheuristicSolver.solve(environment);
        // 5. Clear existing plans for all vehicles
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

        logger.info("Replanning completed. Created {} new vehicle plans", environment.getCurrentVehiclePlans().size());
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

                // Cargar datos del nuevo día
                List<Event> todayOrdersEvents = dataLoader.loadOrdersForDate(today);
                List<Event> todayBlockagesEvents = dataLoader.loadBlockagesForDate(today);

                for (Event e : todayBlockagesEvents) {
                    if (e.getType() != EventType.BLOCKAGE_START)
                        continue;
                    Blockage blockage = (Blockage) e.getData();
                    environment.addBlockage(blockage);
                }

                addEvents(todayOrdersEvents);
                addEvents(todayBlockagesEvents);
                logger.info("Added {} events for the new day {}", todayOrdersEvents.size(), today);
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
                logger.debug("Skipping action for vehicle: " + vehicle.getId() + " as it is scheduled for future time or no current action.");
            }
        }
    }

    private void executeAction(Action action, Vehicle vehicle, SimulationState environment, LocalDateTime nextTick) {
        // Skip if action is already completed
        if (action.getCurrentProgress() >= 1.0) {
            return;
        }

        // Store the previous progress for effect calculations
        double previousProgress = action.getCurrentProgress();

        // Calculate how much of the action should be executed based on time passed
        LocalDateTime actionStart = action.getStartTime();
        LocalDateTime actionEnd = action.getEndTime();
        long totalDurationSeconds = Duration.between(actionStart, actionEnd).toSeconds();

        // If total duration is zero, mark as complete and return
        if (totalDurationSeconds <= 0) {
            action.setCurrentProgress(1.0);
            applyActionEffects(action, vehicle, environment, 1.0, previousProgress);
            return;
        }

        // Calculate elapsed time since action start, capped at action end time
        LocalDateTime effectiveTime = nextTick.isBefore(actionEnd) ? nextTick : actionEnd;
        long elapsedSeconds = Duration.between(actionStart, effectiveTime).toSeconds();

        // Calculate new progress percentage
        double progress = Math.min(1.0, (double) elapsedSeconds / totalDurationSeconds);

        // Update the action's progress
        action.setCurrentProgress(progress);

        // Apply action effects based on type and progress
        applyActionEffects(action, vehicle, environment, progress, previousProgress);
        
        //System.out.println("Executed action " + action.getType() + " progress: " + progress + 
        //                   " [Vehicle: " + vehicle.getId() + 
        //                   ", Position: " + vehicle.getCurrentPosition() + 
        //                   ", Fuel: " + vehicle.getCurrentFuelGal() + 
        //                   ", GLP: " + vehicle.getCurrentGlpM3() + "]");

        // If action is now complete (progress = 1.0), advance to next action in plan
        if (progress >= 1.0) {
            VehiclePlan plan = environment.getCurrentVehiclePlans().get(vehicle.getId());
            if (plan != null) {
                plan.advanceAction();
            }
        }
    }

    private void applyActionEffects(Action action, Vehicle vehicle, SimulationState environment, double progress, double previousProgress) {
        // Apply partial effects based on progress if not completed previously
        if (previousProgress >= progress) {
            return; // No additional effects to apply
        }

        double effectMultiplier = progress - previousProgress;

        // Apply effects based on action type
        switch (action.getType()) {
            case DRIVE:
                // Update vehicle position based on path
                updateVehiclePosition(vehicle, action, progress);

                // Apply fuel consumption
                double fuelToConsume = action.getFuelConsumedGal() * effectMultiplier;
                vehicle.consumeFuel(fuelToConsume);
                break;

            case REFUEL:
                if (progress >= 1.0) {
                    // Only apply refueling at completion
                    vehicle.refuel();
                    logger.info("Vehicle {} refueled to capacity", vehicle.getId());
                }
                break;

            case RELOAD:
                if (progress >= 1.0) {
                    // Only apply GLP loading at completion
                    vehicle.refill(action.getGlpLoaded());
                    logger.info("Vehicle {} loaded {} m³ of GLP", vehicle.getId(), action.getGlpLoaded());

                    // Update depot GLP levels if this is a depot reload
                    Depot depot = environment.getDepotById(action.getDepotId());
                    if (depot != null) {
                        depot.serve(action.getGlpLoaded());
                    }
                }
                break;

            case SERVE:
                if (progress >= 1.0) {
                    // Only apply GLP delivery at completion
                    vehicle.dispense(action.getGlpDelivered());
                    logger.info("Vehicle {} delivered {} m³ of GLP", vehicle.getId(), action.getGlpDelivered());

                    // Find and update order status if needed
                    Order order = environment.getOrderById(action.getOrderId());
                    if (order != null && !order.isDelivered()) {
                        order.recordDelivery(action.getGlpDelivered(), vehicle, action.getEndTime());
                    }
                }
                break;

            case MAINTENANCE:
                // Vehicle is already in maintenance status, no additional effects
                // but we update the position
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    vehicle.setCurrentPosition(action.getPath().get(0));
                }
                break;

            case WAIT:
                // No effects besides keeping the vehicle in place
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    vehicle.setCurrentPosition(action.getPath().get(0));
                }
                break;

            default:
                logger.warn("Unknown action type: {}", action.getType());
                break;
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

            int newX = (int) Math.round(start.getX() + segmentProgress * (end.getX() - start.getX()));
            int newY = (int) Math.round(start.getY() + segmentProgress * (end.getY() - start.getY()));

            vehicle.setCurrentPosition(new Position(newX, newY));
        }
    }
}

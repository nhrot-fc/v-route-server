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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Getter
public class Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

    private final SimulationState environment;
    private final Map<Vehicle, VehiclePlan> vehiclePlans;
    private final PriorityQueue<Event> eventQueue;
    private final int minutesForReplan;
    private final DataLoader dataLoader;

    private LocalDateTime simulationTime;
    private boolean needsReplanning;
    private Duration tickDuration;
    private LocalDateTime lastReplanningTime;

    public Orchestrator(SimulationState environment, Duration tickDuration, int minutesForReplan, DataLoader dataLoader) {
        this.environment = environment;
        this.simulationTime = environment.getCurrentTime();
        this.tickDuration = tickDuration;
        this.vehiclePlans = new HashMap<>();
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
        // TODO: Implement replanning
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
                logger.info("Added {} events for the new day {}",
                        todayOrdersEvents.size(), today);
                break;

            default:
                logger.warn("Unknown event type: " + event.getType());
                break;
        }
    }

    private void executeVehiclePlans(LocalDateTime nextTick) {
        for (Map.Entry<Vehicle, VehiclePlan> entry : vehiclePlans.entrySet()) {
            Vehicle vehicle = entry.getKey();
            VehiclePlan plan = entry.getValue();

            if (vehicle.getStatus() == VehicleStatus.INCIDENT || plan == null) {
                logger.debug("Skipping vehicle " + vehicle.getId() + " (unavailable or no plan)");
                continue;
            }

            for (Action action : plan.getActions()) {
                if (action.getExpectedStartTime().isBefore(nextTick)) {
                    action.execute(vehicle, environment, nextTick);
                    logger.debug("Executed action: " + action + " for vehicle: " + vehicle.getId());
                } else {
                    logger.debug("Skipping action: " + action + " for vehicle: " + vehicle.getId()
                            + " as it is scheduled for future time.");
                }
            }
        }
    }
}

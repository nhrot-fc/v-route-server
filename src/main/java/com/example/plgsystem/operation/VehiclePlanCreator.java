package com.example.plgsystem.operation;

import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.RouteStop;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.*;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehiclePlanCreator {
    private static final Logger logger = LoggerFactory.getLogger(VehiclePlanCreator.class);

    public static VehiclePlan createPlanFromRoute(
            Route route,
            SimulationState state) {

        // Validate input parameters
        if (route == null) {
            return null;
        }

        // Generate empty plan for routes with no stops
        if (route.stops().isEmpty()) {
            return new VehiclePlan(route.vehicleId(), new ArrayList<>(), state.getCurrentTime(), -1);
        }

        // Validate vehicle exists
        Vehicle vehicle = state.getVehicleById(route.vehicleId());
        List<Action> actions = new ArrayList<>();
        if (vehicle == null) {
            logger.error("Vehicle not found for route: {}", route);
            return null;
        }

        // Tracker variables
        Position currentPosition = vehicle.getCurrentPosition();
        double currentFuel = vehicle.getCurrentFuelGal();
        // int currentGlp = vehicle.getCurrentGlpM3();

        // Determine start time, considering current action if any
        LocalDateTime currentTime = state.getCurrentTime();
        if (vehicle.isPerformingAction() && vehicle.getCurrentAction().getType() != ActionType.DRIVE) {
            actions.add(vehicle.getCurrentAction());
            currentTime = vehicle.getCurrentActionEndTime();
        }

        for (RouteStop stop : route.stops()) {
            // Plan loop instructions
            // 1. Drive to the stop -> add a drive action
            // 2. Create action based on the stop type -> add the action
            // 4. Repeat for all stops

            // Validate position
            Position endPosition = stop.getPosition();
            if (endPosition == null) {
                return null;
            }

            if (!currentPosition.equals(endPosition)) {
                // Find path
                List<Position> path = PathFinder.findPath(state, currentPosition, endPosition, currentTime);
                if (path == null) {
                    return null;
                }

                // Calculate fuel and time
                double distanceKm = path.size() - 1;
                double fuelNeeded = calculateFuelFromDistance(distanceKm, vehicle.getGlpCapacityM3(),
                        vehicle.getType());
                int driveTimeMinutes = (int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60);
                Action driveAction = ActionFactory.createDrivingAction(path, fuelNeeded, currentTime,
                        currentTime.plusMinutes(driveTimeMinutes));
                actions.add(driveAction);

                currentFuel -= fuelNeeded;
                currentTime = driveAction.getEndTime();
            }

            if (stop.isOrderStop()) {
                // Validate order exists if order stop
                String orderId = stop.getOrderId();
                Order order = state.getOrderById(orderId);
                if (order == null) {
                    return null;
                }

                int glpToDeliver = stop.getGlpDeliverM3();
                Action serveAction = ActionFactory.createServingAction(endPosition, orderId, glpToDeliver, currentTime);
                actions.add(serveAction);
                // currentGlp -= glpToDeliver;
                currentTime = serveAction.getEndTime();
            } else if (stop.isMaintenanceStop()) {
                // Handle maintenance stop
                String depotId = stop.getDepotId();
                Depot depot = state.getDepotById(depotId);
                if (depot == null) {
                    return null;
                }

                // Create a drive action to the depot
                List<Position> path = PathFinder.findPath(state, currentPosition, depot.getPosition(), currentTime);
                if (path == null) {
                    return null;
                }
                double distanceKm = path.size() - 1;
                double fuelNeeded = calculateFuelFromDistance(distanceKm, vehicle.getGlpCapacityM3(), vehicle.getType());
                int driveTimeMinutes = (int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60);
                Action driveAction = ActionFactory.createDrivingAction(path, fuelNeeded, currentTime,
                        currentTime.plusMinutes(driveTimeMinutes));
                actions.add(driveAction);
                currentFuel -= fuelNeeded;
                currentTime = driveAction.getEndTime();

                // Create a maintenance action that lasts 24 hours
                Duration maintenanceDuration = Duration.ofHours(Constants.MAINTENANCE_DURATION_HOURS);
                Action maintenanceAction = ActionFactory.createMaintenanceAction(
                        endPosition,
                        maintenanceDuration,
                        currentTime);
                actions.add(maintenanceAction);

                // Vehicle is refueled and refilled during maintenance
                currentFuel = vehicle.getFuelCapacityGal();
                currentTime = maintenanceAction.getEndTime();
            } else {
                // Validate depot exists if depot stop
                String depotId = stop.getDepotId();
                Depot depot = state.getDepotById(depotId);
                if (depot == null) {
                    return null;
                }
                int glpToLoad = stop.getGlpLoadM3();
                Action reloadAction = ActionFactory.createRefillingAction(depotId, endPosition, currentTime, glpToLoad);
                actions.add(reloadAction);
                currentTime = reloadAction.getEndTime();
                // currentGlp += glpToLoad;
                double fuelToRefill = vehicle.getFuelCapacityGal() - currentFuel;
                Action refuelAction = ActionFactory.createRefuelingAction(depotId, endPosition, currentTime,
                        fuelToRefill);
                actions.add(refuelAction);

                currentFuel = vehicle.getFuelCapacityGal();
                currentTime = refuelAction.getEndTime();
            }

            currentPosition = endPosition;
        }
        return new VehiclePlan(route.vehicleId(), actions, state.getCurrentTime(), 0);
    }

    private static double calculateFuelFromDistance(double distanceKm, int glpCapacityM3, VehicleType vehicleType) {
        return (Constants.GLP_DENSITY_M3_TON * glpCapacityM3 + vehicleType.getTareWeightTon())
                * (distanceKm / Constants.VEHICLE_AVG_SPEED);
    }

    public static VehiclePlan createPlanToMainDepot(
            Vehicle vehicle,
            SimulationState state) {

        // Validate input parameters
        if (vehicle == null || state == null) {
            return null;
        }

        // Get main depot position
        Depot mainDepot = state.getMainDepot();
        if (mainDepot == null) {
            return null;
        }

        Position depotPosition = mainDepot.getPosition();
        if (depotPosition == null) {
            return null;
        }

        List<Action> actions = new ArrayList<>();
        Position currentPosition = vehicle.getCurrentPosition();
        double currentFuel = vehicle.getCurrentFuelGal();

        // Determine start time, considering current action if any
        LocalDateTime currentTime = state.getCurrentTime();
        if (vehicle.isPerformingAction() && vehicle.getCurrentActionEndTime() != null) {
            currentTime = vehicle.getCurrentActionEndTime();
            logger.debug("Vehicle {} has ongoing action, plan to main depot will start at {}",
                    vehicle.getId(), currentTime);
        }

        if (!currentPosition.equals(depotPosition)) {
            // Find path to main depot
            List<Position> path = PathFinder.findPath(state, currentPosition, depotPosition, currentTime);
            if (path == null) {
                return null;
            }

            // Calculate fuel and time
            double distanceKm = path.size() - 1;
            double fuelNeeded = calculateFuelFromDistance(distanceKm, vehicle.getGlpCapacityM3(), vehicle.getType());
            int driveTimeMinutes = (int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60);
            Action driveAction = ActionFactory.createDrivingAction(path, fuelNeeded, currentTime,
                    currentTime.plusMinutes(driveTimeMinutes));
            actions.add(driveAction);

            currentFuel -= fuelNeeded;
            currentTime = driveAction.getEndTime();
        }
        // Create refueling action at depot
        Action refuelAction = ActionFactory.createRefuelingAction(mainDepot.getId(), depotPosition, currentTime,
                vehicle.getFuelCapacityGal() - currentFuel);
        actions.add(refuelAction);
        currentFuel = vehicle.getFuelCapacityGal();
        currentTime = refuelAction.getEndTime();

        // Create refilling action at depot
        Action refillAction = ActionFactory.createRefillingAction(mainDepot.getId(), depotPosition, currentTime,
                vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3());
        actions.add(refillAction);
        currentTime = refillAction.getEndTime();

        return new VehiclePlan(vehicle.getId(), actions, state.getCurrentTime(), 0);
    }

    /**
     * Creates a plan for a vehicle with an incident.
     * The plan includes waiting during immobilization and potentially returning to
     * the main depot.
     * 
     * @param vehicle  The vehicle with the incident
     * @param incident The incident that occurred
     * @param state    The current simulation state
     * @return A VehiclePlan for the incident handling
     */
    public static VehiclePlan createPlanForIncident(
            Vehicle vehicle,
            Incident incident,
            SimulationState state) {

        // Validate input parameters
        if (vehicle == null || incident == null || state == null) {
            logger.error("Cannot create incident plan: invalid input parameters");
            return null;
        }

        List<Action> actions = new ArrayList<>();

        // Determine start time, considering current action if any
        LocalDateTime currentTime = state.getCurrentTime();
        if (vehicle.isPerformingAction() && vehicle.getCurrentActionEndTime() != null) {
            currentTime = vehicle.getCurrentActionEndTime();
            logger.debug("Vehicle {} has ongoing action, incident plan will start at {}",
                    vehicle.getId(), currentTime);
        }

        Position incidentPosition = vehicle.getCurrentPosition();

        // Calculate immobilization end time
        LocalDateTime immobilizationEndTime = incident.getImmobilizationEndTime();

        // Add a wait action for the immobilization period only if needed
        if (immobilizationEndTime.isAfter(currentTime)) {
            Duration waitDuration = Duration.between(currentTime, immobilizationEndTime);
            long waitTimeMinutes = waitDuration.toMinutes();
            if (waitTimeMinutes > 0) {
                Action waitAction = ActionFactory.createIdleAction(
                        incidentPosition,
                        waitDuration,
                        currentTime);
                actions.add(waitAction);
                currentTime = waitAction.getEndTime();
            }
        }

        // After immobilization, if return to depot is required, create a plan to return
        if (incident.isReturnToDepotRequired()) {
            Depot mainDepot = state.getMainDepot();
            if (mainDepot == null) {
                logger.error("Cannot create incident plan: main depot not found");
                return null;
            }

            Position depotPosition = mainDepot.getPosition();
            if (depotPosition == null) {
                logger.error("Cannot create incident plan: main depot position not defined");
                return null;
            }

            // Only create drive action if not already at the depot
            if (!incidentPosition.equals(depotPosition)) {
                // Find path to main depot
                List<Position> path = PathFinder.findPath(state, incidentPosition, depotPosition, currentTime);
                if (path == null) {
                    logger.error("Cannot create incident plan: no path found to main depot");
                    return null;
                }

                // Calculate fuel and time
                double distanceKm = path.size() - 1;
                double fuelNeeded = calculateFuelFromDistance(distanceKm, vehicle.getGlpCapacityM3(),
                        vehicle.getType());
                int driveTimeMinutes = (int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60);

                // Create drive action
                Action driveAction = ActionFactory.createDrivingAction(
                        path,
                        fuelNeeded,
                        currentTime,
                        currentTime.plusMinutes(driveTimeMinutes));
                actions.add(driveAction);
                currentTime = driveAction.getEndTime();
            }

            // Add a maintenance action at the depot for the repair period
            LocalDateTime availabilityTime = incident.getAvailabilityTime();
            if (availabilityTime.isAfter(currentTime)) {
                Duration repairDuration = Duration.between(currentTime, availabilityTime);
                long waitTimeMinutes = repairDuration.toMinutes();
                if (waitTimeMinutes > 0) {
                    Action repairAction = ActionFactory.createMaintenanceAction(
                            depotPosition,
                            repairDuration,
                            currentTime);
                    actions.add(repairAction);
                }
            }
        }

        // Return the plan
        return new VehiclePlan(vehicle.getId(), actions, state.getCurrentTime(), 0);
    }
}
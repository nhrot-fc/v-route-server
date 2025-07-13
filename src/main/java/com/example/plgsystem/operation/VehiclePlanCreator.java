package com.example.plgsystem.operation;

import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.RouteStop;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.*;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehiclePlanCreator {
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

        // Tracker variables
        Position currentPosition = vehicle.getCurrentPosition();
        double currentFuel = vehicle.getCurrentFuelGal();
        // int currentGlp = vehicle.getCurrentGlpM3();
        LocalDateTime currentTime = state.getCurrentTime();

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
        LocalDateTime currentTime = state.getCurrentTime();
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
}
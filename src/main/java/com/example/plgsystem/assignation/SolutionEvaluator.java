package com.example.plgsystem.assignation;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.*;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionType;
import com.example.plgsystem.simulation.SimulationState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class SolutionEvaluator {
    // Cost factors
    private static final double COST_PER_MINUTE = 10.0;
    private static final double INCOMPLETE_ORDER_PENALTY = 10000.0;
    private static final double LATE_DELIVERY_PENALTY = 10000.0;
    private static final double COST_PER_KM = 0.000001;

    // Security factor for distance calculation
    private static final double SECURITY_FACTOR = 1.25; // 50% safety margin for distance

    public static SolutionCost evaluate(Solution solution, SimulationState state) {
        Map<String, Route> routes = solution.getRoutes();
        double timeCost = 0;
        double distanceCost = 0;
        double lateDeliveryCost = 0;
        double incompleteOrderCost = 0;

        // Track which orders have been served and how much
        Map<String, Integer> ordersRemainingGlp = new HashMap<>();
        Map<String, Integer> depotsGlpState = new HashMap<>();

        // Initialize order state from simulation state
        for (Order order : state.getOrders()) {
            ordersRemainingGlp.put(order.getId(), order.getRemainingGlpM3());
        }
        for (Depot depot : state.getAuxDepots()) {
            depotsGlpState.put(depot.getId(), depot.getGlpCapacityM3());
        }
        depotsGlpState.put(state.getMainDepot().getId(), state.getMainDepot().getGlpCapacityM3());

        for (Map.Entry<String, Route> entry : routes.entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            Vehicle vehicle = state.getVehicleById(vehicleId);

            if (vehicle == null || route == null) {
                return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
            }

            SolutionCost routeCost = evaluateRoute(route, vehicle, state, ordersRemainingGlp, depotsGlpState);
            if (routeCost.invalidCost() > 0) {
                return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
            }

            // Track the latest delivery time
            timeCost = Math.max(timeCost, routeCost.timeCost());
            lateDeliveryCost += routeCost.lateDeliveryCost();
            distanceCost += routeCost.distanceCost();
        }

        // Check for incomplete orders
        for (Map.Entry<String, Integer> entry : ordersRemainingGlp.entrySet()) {
            int remainingGlp = entry.getValue();
            if (remainingGlp > 0) {
                incompleteOrderCost += INCOMPLETE_ORDER_PENALTY; // Apply penalty for incomplete orders
            }
        }

        return new SolutionCost(timeCost, distanceCost, lateDeliveryCost, incompleteOrderCost, 0.0);
    }

    private static SolutionCost evaluateRoute(Route route, Vehicle vehicle, SimulationState state,
            Map<String, Integer> ordersRemainingGlp, Map<String, Integer> depotsGlpState) {
        String mainDepotId = state.getMainDepot().getId();
        double totalDistance = 0;
        int totalLateDeliveries = 0;

        // Consider the vehicle's current action when determining start time
        LocalDateTime startTime = state.getCurrentTime();
        if (vehicle.isPerformingAction() && vehicle.getCurrentAction().getType() != ActionType.DRIVE) {
            Action currentAction = vehicle.getCurrentAction();
            if (currentAction != null && currentAction.getEndTime().isAfter(startTime)) {
                // Route can only start after the current action finishes
                startTime = currentAction.getEndTime();
            }
        }

        LocalDateTime lastDeliveryTime = startTime;
        LocalDateTime currentTime = startTime;

        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();

        for (RouteStop stop : route.stops()) {
            Position nextPosition = stop.getPosition();
            double distanceKm = currentPosition.distanceTo(nextPosition) * SECURITY_FACTOR;
            double timeSeconds = (distanceKm / Constants.VEHICLE_AVG_SPEED) * 3600;
            double fuelNeeded = calculateFuelNeeded(distanceKm, currentGlp, vehicle.getType());

            if (fuelNeeded > currentFuel) {
                return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
            }

            // Update costs
            totalDistance += distanceKm;
            currentTime = currentTime.plusSeconds((long) timeSeconds);
            currentFuel -= fuelNeeded;
            currentPosition = nextPosition;

            // Handle stop logic
            if (stop.isOrderStop()) {
                String orderId = stop.getOrderId();
                Order order = state.getOrderById(orderId);
                if (order == null) {
                    return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
                }
                int remainingGlp = ordersRemainingGlp.getOrDefault(orderId, 0);
                int glpToDeliver = stop.getGlpDeliverM3();

                // Update order state
                ordersRemainingGlp.put(orderId, Math.max(0, remainingGlp - glpToDeliver));
                // Update vehicle GLP
                currentGlp -= glpToDeliver;

                // Update costs for late deliveries
                if (currentTime.isAfter(order.getDeadlineTime())) {
                    totalLateDeliveries += 1;
                }

                // Update service time
                currentTime = currentTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);

                // Update last delivery time (only for actual deliveries)
                if (glpToDeliver > 0) {
                    lastDeliveryTime = currentTime;
                }
            } else if (stop.isMaintenanceStop()) {
                currentTime = currentTime.plusHours(Constants.MAINTENANCE_DURATION_HOURS);
                currentFuel = vehicle.getFuelCapacityGal();
                currentGlp = vehicle.getGlpCapacityM3();
            } else {
                String depotId = stop.getDepotId();
                Depot depot = state.getDepotById(depotId);
                if (depot == null) {
                    return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
                }
                // Check if depot has enough GLP
                int depotGlpAvailable = depotsGlpState.getOrDefault(depotId, 0);
                // Update depot GLP state
                depotsGlpState.put(depotId, Math.max(0, depotGlpAvailable - currentGlp));
                // Update vehicle GLP
                currentGlp += stop.getGlpLoadM3();
                currentFuel = vehicle.getFuelCapacityGal();
                if (depotId.equals(mainDepotId)) {
                    currentTime = currentTime.plusMinutes(Constants.RELOAD_REFUEL_DURATION_MINUTES_MAIN_DEPOT);
                } else {
                    currentTime = currentTime.plusMinutes(Constants.RELOAD_REFUEL_DURATION_MINUTES);
                }
            }

            if (currentGlp > vehicle.getGlpCapacityM3() || currentGlp < 0) {
                return new SolutionCost(0.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY);
            }
        }

        // Calculate time cost: time to complete last delivery (from start of solution)
        long timeCostMinutes = Duration.between(state.getCurrentTime(), lastDeliveryTime).toMinutes();

        return new SolutionCost(
                timeCostMinutes * COST_PER_MINUTE,
                totalDistance * COST_PER_KM,
                totalLateDeliveries * LATE_DELIVERY_PENALTY,
                0,
                0);
    }

    private static double calculateFuelNeeded(double distanceKm, int glpVolumeM3, VehicleType vehicleType) {
        double combinedWeight = (glpVolumeM3 * Constants.GLP_DENSITY_M3_TON) + vehicleType.getTareWeightTon();
        return (distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR;
    }
}

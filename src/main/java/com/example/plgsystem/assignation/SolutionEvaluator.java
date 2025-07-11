package com.example.plgsystem.assignation;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.*;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import java.time.LocalDateTime;
import java.util.*;

public class SolutionEvaluator {
    // Penalties
    private static final double INCOMPLETE_ORDER_PENALTY = 5000.0;
    private static final double LATE_DELIVERY_PENALTY = 5000.0;
    private static final double DISTANCE_PENALTY_PER_KM = 0.01;

    private static final int DELIVER_SERVICE_TIME_MIN = Constants.GLP_SERVE_DURATION_MINUTES;
    private static final int LOAD_SERVICE_TIME_MIN = Constants.VEHICLE_GLP_TRANSFER_DURATION_MINUTES;

    public static double evaluate(Solution solution, SimulationState state) {
        Map<String, Route> routes = solution.getRoutes();
        double totalCost = 0;

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

        // Evaluate each route
        for (String vehicleId : routes.keySet()) {
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                return Double.POSITIVE_INFINITY;
            }

            Route route = routes.get(vehicleId);
            double routeCost = evaluateRoute(route, vehicle, state, ordersRemainingGlp, depotsGlpState);

            if (routeCost == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            }

            totalCost += routeCost;
        }

        // Check if any orders remain unserved or partially served
        for (Map.Entry<String, Integer> entry : ordersRemainingGlp.entrySet()) {
            int remaining = entry.getValue();
            if (remaining > 0) {
                totalCost += remaining * INCOMPLETE_ORDER_PENALTY;
            }
        }

        return totalCost;
    }

    private static double evaluateRoute(Route route, Vehicle vehicle, SimulationState state,
            Map<String, Integer> ordersRemainingGlp, Map<String, Integer> depotsGlpState) {
        double routeCost = 0;

        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        LocalDateTime currentTime = state.getCurrentTime();

        for (RouteStop stop : route.stops()) {
            Position targetPosition = stop.getPosition();

            // Find path considering blockages
            List<Position> path = PathFinder.findPath(state, currentPosition, targetPosition, currentTime);
            if (path == null) {
                return Double.POSITIVE_INFINITY; // No path available (blockage)
            }

            double distance = path.size() - 1;

            // Calculate fuel needed based on distance, cargo weight, and vehicle type
            double fuelNeeded = calculateFuelNeeded(distance, currentGlp, vehicle.getType());
            if (currentFuel < fuelNeeded) {
                return Double.POSITIVE_INFINITY; // Not enough fuel
            }

            // Update fuel and apply distance penalty
            currentFuel -= fuelNeeded;
            routeCost += distance * DISTANCE_PENALTY_PER_KM;

            // Calculate travel time
            int timeNeededSeconds = (int) (distance * 3600 / Constants.VEHICLE_AVG_SPEED);
            currentTime = currentTime.plusSeconds(timeNeededSeconds);

            // Update position
            currentPosition = targetPosition;

            if (stop.isOrderStop()) {
                // Order stop
                Order order = state.getOrderById(stop.getOrderId());
                if (order == null) {
                    return Double.POSITIVE_INFINITY;
                }

                // Check if we have enough GLP for delivery
                int glpToDeliver = stop.getGlpDeliverM3();
                if (currentGlp < glpToDeliver) {
                    return Double.POSITIVE_INFINITY; // Not enough GLP to fulfill this delivery
                }

                // Update GLP
                currentGlp -= glpToDeliver;

                // Update remaining GLP for this order
                int orderRemainingGlp = ordersRemainingGlp.get(stop.getOrderId()) - glpToDeliver;
                if (orderRemainingGlp < 0) {
                    return Double.POSITIVE_INFINITY; // Delivering more than needed
                }
                ordersRemainingGlp.put(stop.getOrderId(), orderRemainingGlp);

                // Check if delivery is late
                if (order.getDeadlineTime() != null && currentTime.isAfter(order.getDeadlineTime())) {
                    routeCost += LATE_DELIVERY_PENALTY;
                }

                // Add service time
                currentTime = currentTime.plusMinutes(DELIVER_SERVICE_TIME_MIN);

            } else {
                // Depot stop
                String depotId = stop.getDepotId();
                Depot depot = state.getDepotById(depotId);
                if (depot == null) {
                    return Double.POSITIVE_INFINITY;
                }

                int glpToLoad = stop.getGlpLoadM3();
                if (glpToLoad > 0 && !depot.isMain()) {
                    int depotGlp = depotsGlpState.get(depotId);
                    if (depotGlp < glpToLoad) {
                        return Double.POSITIVE_INFINITY;
                    }
                    depotsGlpState.put(depotId, depotGlp - glpToLoad);
                    currentGlp += glpToLoad;
                }

                // Refuel vehicle
                currentFuel = vehicle.getFuelCapacityGal();

                // Add service time
                currentTime = currentTime.plusMinutes(LOAD_SERVICE_TIME_MIN);
            }
        }

        return routeCost;
    }

    private static double calculateFuelNeeded(double distanceKm, int glpVolumeM3, VehicleType vehicleType) {
        double combinedWeight = (glpVolumeM3 * Constants.GLP_DENSITY_M3_TON) + vehicleType.getTareWeightTon();
        return (distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR;
    }
}
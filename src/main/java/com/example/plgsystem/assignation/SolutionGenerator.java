package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.simulation.SimulationState;

import java.util.Comparator;

public class SolutionGenerator {
    // Safety factor for fuel consumption estimates
    private static final double FUEL_SAFETY_FACTOR = 1.5;
    // Threshold to decide when to visit a depot for refill
    private static final double GLP_THRESHOLD_RATIO = 0.3;
    private static final double FUEL_THRESHOLD_RATIO = 0.3;

    public static Solution generateSolution(SimulationState state, Map<String, List<DeliveryPart>> assignments) {
        Map<String, Integer> ordersState = new HashMap<>();
        Map<String, Integer> depotsState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();

        // Initialize depots state with current GLP levels
        for (Depot depot : state.getAuxDepots()) {
            depotsState.put(depot.getId(), depot.getCurrentGlpM3());
        }
        depotsState.put(state.getMainDepot().getId(), state.getMainDepot().getCurrentGlpM3());

        // Initialize orders state with remaining GLP to be delivered
        for (Order order : state.getOrders()) {
            ordersState.put(order.getId(), order.getRemainingGlpM3());
        }

        // Process each vehicle's assignments
        for (String vehicleId : assignments.keySet()) {
            List<DeliveryPart> deliveries = assignments.get(vehicleId);
            if (deliveries == null || deliveries.isEmpty()) {
                continue;
            }

            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                continue;
            }

            // Create route for this vehicle
            List<RouteStop> stops = new ArrayList<>();

            // Track vehicle's current state
            int currentGlp = vehicle.getCurrentGlpM3();
            double currentFuel = vehicle.getCurrentFuelGal();
            Position currentPos = vehicle.getCurrentPosition();

            // Sort deliveries by deadline
            deliveries.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));

            for (DeliveryPart delivery : deliveries) {
                Order order = state.getOrderById(delivery.getOrderId());
                if (order == null || order.isDelivered()) {
                    continue;
                }

                // Skip if order position is blocked
                if (state.isPositionBlockedAt(order.getPosition(), state.getCurrentTime())) {
                    continue;
                }

                // Check if we need to visit a depot before delivery
                double distanceToOrder = currentPos.distanceTo(order.getPosition());
                double fuelNeeded = estimateFuelConsumption(vehicle, distanceToOrder);

                // Visit depot if GLP or fuel is low
                if (currentGlp < delivery.getGlpDeliverM3() ||
                        needsRefill(vehicle, currentGlp) ||
                        needsRefuel(vehicle, currentFuel, fuelNeeded)) {

                    // Find nearest non-blocked depot
                    Depot nearestDepot = findNearestAccessibleDepot(state, currentPos);
                    if (nearestDepot != null) {
                        // Calculate fuel to reach depot
                        double distanceToDepot = currentPos.distanceTo(nearestDepot.getPosition());
                        double fuelToDepot = estimateFuelConsumption(vehicle, distanceToDepot);

                        // Skip if vehicle can't reach depot
                        if (currentFuel >= fuelToDepot) {
                            // Add depot stop
                            int glpToLoad = Math.min(
                                    vehicle.getGlpCapacityM3() - currentGlp,
                                    depotsState.get(nearestDepot.getId()));

                            if (glpToLoad > 0) {
                                stops.add(new RouteStop(
                                        nearestDepot.getId(),
                                        glpToLoad));

                                // Update depot state
                                if (!nearestDepot.isMain()) {
                                    depotsState.put(
                                            nearestDepot.getId(),
                                            depotsState.get(nearestDepot.getId()) - glpToLoad);
                                }

                                // Update vehicle state
                                currentGlp += glpToLoad;
                                currentFuel = vehicle.getFuelCapacityGal(); // Refueled
                                currentPos = nearestDepot.getPosition();
                            }
                        }
                    }
                }

                // Proceed with delivery if we have enough GLP and fuel
                double distanceToOrderNow = currentPos.distanceTo(order.getPosition());
                double fuelToOrder = estimateFuelConsumption(vehicle, distanceToOrderNow);

                if (currentGlp >= delivery.getGlpDeliverM3() && currentFuel >= fuelToOrder) {
                    // Add delivery stop
                    stops.add(new RouteStop(
                            order.getId(),
                            order.getDeadlineTime(),
                            delivery.getGlpDeliverM3()
                    ));

                    // Update order state
                    ordersState.put(
                            order.getId(),
                            ordersState.get(order.getId()) - delivery.getGlpDeliverM3());

                    // Update vehicle state
                    currentGlp -= delivery.getGlpDeliverM3();
                    currentFuel -= fuelToOrder;
                    currentPos = order.getPosition();
                }
            }

            // Return to main depot at end of route if needed
            if (!stops.isEmpty()) {
                routes.put(vehicleId, new Route(vehicleId, stops, state.getCurrentTime()));
            }
        }

        return new Solution(ordersState, depotsState, routes);
    }

    private static double estimateFuelConsumption(Vehicle vehicle, double distance) {
        // Basic estimate with safety factor
        return vehicle.calculateFuelNeeded(distance) * FUEL_SAFETY_FACTOR;
    }

    private static boolean needsRefill(Vehicle vehicle, int currentGlp) {
        return currentGlp < vehicle.getGlpCapacityM3() * GLP_THRESHOLD_RATIO;
    }

    private static boolean needsRefuel(Vehicle vehicle, double currentFuel, double plannedConsumption) {
        return currentFuel < plannedConsumption ||
                currentFuel < vehicle.getFuelCapacityGal() * FUEL_THRESHOLD_RATIO;
    }

    private static Depot findNearestAccessibleDepot(SimulationState state, Position position) {
        Depot nearest = null;
        double minDistance = Double.MAX_VALUE;

        // Check main depot
        Depot mainDepot = state.getMainDepot();
        if (!state.isPositionBlockedAt(mainDepot.getPosition(), state.getCurrentTime())) {
            nearest = mainDepot;
            minDistance = position.distanceTo(mainDepot.getPosition());
        }

        // Check auxiliary depots
        for (Depot depot : state.getAuxDepots()) {
            if (!state.isPositionBlockedAt(depot.getPosition(), state.getCurrentTime())) {
                double distance = position.distanceTo(depot.getPosition());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = depot;
                }
            }
        }

        return nearest;
    }
}
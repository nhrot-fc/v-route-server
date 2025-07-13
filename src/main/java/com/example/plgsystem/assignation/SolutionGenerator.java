package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class SolutionGenerator {
    private static final double FUEL_SAFETY_FACTOR = 1.5;
    private static final double FUEL_THRESHOLD_RATIO = 0.3;

    public static Solution generateSolution(SimulationState state, Map<String, List<DeliveryPart>> assignments) {
        Map<String, Integer> depotsGlpState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();
        // Initialize depot GLP states
        for (Depot depot : state.getAuxDepots()) {
            depotsGlpState.put(depot.getId(), depot.getGlpCapacityM3());
        }
        depotsGlpState.put(state.getMainDepot().getId(), state.getMainDepot().getGlpCapacityM3());

        LocalDateTime startTime = state.getCurrentTime();
        for (Map.Entry<String, List<DeliveryPart>> entry : assignments.entrySet()) {
            String vehicleId = entry.getKey();
            List<DeliveryPart> deliveryParts = entry.getValue();

            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null || deliveryParts.isEmpty()) {
                continue;
            }
            Route route = buildRoute(vehicle, state, startTime, deliveryParts, depotsGlpState);
            if (route == null) {
                continue; // Skip this vehicle if no valid route could be built
            }
            routes.put(vehicleId, route);
        }

        return new Solution(routes, state);
    }

    private static Route buildRoute(Vehicle vehicle, SimulationState state, LocalDateTime startTime,
            List<DeliveryPart> deliveryParts, Map<String, Integer> depotsGlpState) {

        List<RouteStop> stops = new ArrayList<>();
        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        LocalDateTime currentTime = startTime;

        for (DeliveryPart part : deliveryParts) {
            if (currentGlp < part.getGlpDeliverM3()
                    || currentFuel <= vehicle.getGlpCapacityM3() * FUEL_THRESHOLD_RATIO) {
                // Find nearest depot to load GLP
                int glpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
                Depot nearestDepot = findNearesDepot(glpToLoad, currentPosition, state, currentTime,
                        depotsGlpState);
                if (nearestDepot == null) {
                    return new Route(vehicle.getId(), stops, startTime); // No valid depot found
                }

                // Move to depot
                double distanceToDepot = currentPosition.distanceTo(nearestDepot.getPosition());
                currentFuel -= calculateFuelNeeded(distanceToDepot, currentGlp, vehicle.getType()) * FUEL_SAFETY_FACTOR;
                if (currentFuel < 0) {
                    return null;
                }
                currentPosition = nearestDepot.getPosition();
                currentTime = currentTime.plusMinutes((long) (distanceToDepot / Constants.VEHICLE_AVG_SPEED * 60));

                // Load GLP at depot to max capacity
                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), glpToLoad));
                currentTime = currentTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
                depotsGlpState.put(nearestDepot.getId(),
                        depotsGlpState.get(nearestDepot.getId()) - glpToLoad);
                currentGlp += glpToLoad;
            }

            // Move to delivery position
            String orderId = part.getOrderId();
            Order order = state.getOrderById(orderId);
            Position deliveryPosition = order.getPosition();
            double distanceToDelivery = currentPosition.distanceTo(deliveryPosition);
            currentFuel -= calculateFuelNeeded(distanceToDelivery, currentGlp, vehicle.getType()) * FUEL_SAFETY_FACTOR;
            if (currentFuel < 0) {
                return null;
            }
            currentPosition = deliveryPosition;
            currentTime = currentTime.plusMinutes((long) (distanceToDelivery / Constants.VEHICLE_AVG_SPEED * 60));
            stops.add(new RouteStop(currentPosition, orderId, order.getDeadlineTime(), part.getGlpDeliverM3()));
            currentTime = currentTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            currentGlp -= part.getGlpDeliverM3();
        }

        // Return to depot main depot
        Depot mainDepot = state.getMainDepot();
        double distanceToMainDepot = currentPosition.distanceTo(mainDepot.getPosition());
        currentFuel -= calculateFuelNeeded(distanceToMainDepot, currentGlp, vehicle.getType()) * FUEL_SAFETY_FACTOR;
        if (currentFuel < 0) {
            return null;
        }
        currentPosition = mainDepot.getPosition();
        currentTime = currentTime.plusMinutes((long) (distanceToMainDepot / Constants.VEHICLE_AVG_SPEED * 60));
        stops.add(new RouteStop(currentPosition, mainDepot.getId(), 0)); // No GLP load at main depot

        return new Route(vehicle.getId(), stops, startTime);
    }

    private static double calculateFuelNeeded(double distance, int currentGlp, VehicleType vehicleType) {
        double fuelConsumption = distance
                * (currentGlp * Constants.GLP_DENSITY_M3_TON + vehicleType.getTareWeightTon());
        return fuelConsumption / Constants.CONSUMPTION_FACTOR;
    }

    private static Depot findNearesDepot(int glpRequest, Position position, SimulationState state, LocalDateTime time,
            Map<String, Integer> depotsGlpState) {
        Depot nearestDepot = null;
        double minDistance = Double.MAX_VALUE;

        List<Depot> allDepots = new ArrayList<>(state.getAuxDepots());
        allDepots.add(state.getMainDepot());

        for (Depot depot : allDepots) {
            if (depot.getGlpCapacityM3() < glpRequest || depotsGlpState.get(depot.getId()) < glpRequest) {
                continue;
            }
            double distance = position.distanceTo(depot.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearestDepot = depot;
            }
        }

        if (nearestDepot == null || minDistance == Double.MAX_VALUE) {
            return null;
        }

        return nearestDepot;
    }
}
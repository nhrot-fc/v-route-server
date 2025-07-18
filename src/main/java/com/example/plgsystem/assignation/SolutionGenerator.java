package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class SolutionGenerator {
    private static final double FUEL_SAFETY_FACTOR = 1.3; // Reduced from 1.5 to make routes more feasible
    private static final double FUEL_THRESHOLD_RATIO = 0.4; // If fuel is below this ratio, consider visiting a depot
    private static final int DEPOT_FUEL_REFILL_TIME_MINUTES = Constants.REFUEL_DURATION_MINUTES;

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
            if (vehicle == null) {
                routes.put(vehicleId, new Route(vehicleId, new ArrayList<>(), startTime));
                continue;
            }

            // Sort deliveries by deadline for more efficient routes
            deliveryParts.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));

            Route route = buildRoute(vehicle, state, startTime, deliveryParts, depotsGlpState);
            routes.put(vehicleId, route != null ? route : new Route(vehicleId, new ArrayList<>(), startTime));
        }

        return new Solution(routes, state);
    }

    private static Route buildRoute(Vehicle vehicle, SimulationState state, LocalDateTime startTime,
            List<DeliveryPart> deliveryParts, Map<String, Integer> depotsGlpState) {

        List<RouteStop> stops = new ArrayList<>();
        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        double maxFuel = vehicle.getFuelCapacityGal();
        LocalDateTime currentTime = startTime;

        // Process each delivery
        for (int i = 0; i < deliveryParts.size(); i++) {
            DeliveryPart part = deliveryParts.get(i);
            Order order = state.getOrderById(part.getOrderId());
            if (order == null)
                continue;

            // Check if we need more GLP for this delivery
            if (currentGlp < part.getGlpDeliverM3()) {
                // Find depot to load GLP
                int glpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
                Depot depot = findNearestDepot(currentPosition, glpToLoad, state, depotsGlpState);

                if (depot == null) {
                    return null; // No suitable depot found
                }

                // Calculate fuel needed to reach depot
                double distanceToDepot = currentPosition.distanceTo(depot.getPosition());
                double fuelToDepot = calculateFuelNeeded(distanceToDepot, currentGlp, vehicle.getType());

                if (currentFuel < fuelToDepot) {
                    return null; // Can't reach depot with current fuel
                }

                // Move to depot
                currentFuel -= fuelToDepot;
                currentPosition = depot.getPosition();
                currentTime = currentTime.plusMinutes((long) (distanceToDepot / Constants.VEHICLE_AVG_SPEED * 60));

                // Load GLP and refuel
                stops.add(new RouteStop(currentPosition, depot.getId(), glpToLoad));
                currentTime = currentTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
                depotsGlpState.put(depot.getId(), depotsGlpState.get(depot.getId()) - glpToLoad);
                currentGlp += glpToLoad;
                currentFuel = maxFuel; // Refill to max
                currentTime = currentTime.plusMinutes(DEPOT_FUEL_REFILL_TIME_MINUTES);
            }

            // Calculate fuel needed for next delivery
            Position deliveryPosition = order.getPosition();
            double distanceToDelivery = currentPosition.distanceTo(deliveryPosition);
            double fuelToDelivery = calculateFuelNeeded(distanceToDelivery, currentGlp, vehicle.getType());

            // Look ahead to next position (either next delivery or main depot)
            Position nextPosition;
            double nextDistance;

            if (i < deliveryParts.size() - 1) {
                // There's another delivery after this
                Order nextOrder = state.getOrderById(deliveryParts.get(i + 1).getOrderId());
                nextPosition = nextOrder != null ? nextOrder.getPosition() : state.getMainDepot().getPosition();
            } else {
                // This is the last delivery, next stop is main depot
                nextPosition = state.getMainDepot().getPosition();
            }

            nextDistance = deliveryPosition.distanceTo(nextPosition);
            double fuelForNextLeg = calculateFuelNeeded(nextDistance,
                    currentGlp - part.getGlpDeliverM3(), // GLP after delivery
                    vehicle.getType());

            // Find nearest depot for refueling
            Depot nearestDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);
            double distanceToNearestDepot = (nearestDepot != null)
                    ? currentPosition.distanceTo(nearestDepot.getPosition())
                    : Double.MAX_VALUE;
            double fuelToNearestDepot = calculateFuelNeeded(distanceToNearestDepot, currentGlp, vehicle.getType());

            // Check if we need to refuel before delivery
            boolean needRefuel = false;

            // Case 1: Not enough fuel to reach delivery
            if (currentFuel < fuelToDelivery) {
                needRefuel = true;
            }
            // Case 2: Enough for delivery but not for next leg and below threshold
            else if (currentFuel < fuelToDelivery + fuelForNextLeg &&
                    currentFuel / maxFuel < FUEL_THRESHOLD_RATIO) {
                needRefuel = true;
            }
            // Case 3: Fuel critically low - barely enough to reach depot
            else if (currentFuel < fuelToNearestDepot * 1.5 &&
                    currentFuel / maxFuel < FUEL_THRESHOLD_RATIO / 2) {
                needRefuel = true;
            }

            if (needRefuel) {
                if (nearestDepot == null || currentFuel < fuelToNearestDepot) {
                    return null; // Can't reach depot
                }

                // Go to depot for fuel
                currentFuel -= fuelToNearestDepot;
                currentPosition = nearestDepot.getPosition();
                currentTime = currentTime
                        .plusMinutes((long) (distanceToNearestDepot / Constants.VEHICLE_AVG_SPEED * 60));

                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), 0)); // Just for fuel
                currentFuel = maxFuel; // Refill
                currentTime = currentTime.plusMinutes(DEPOT_FUEL_REFILL_TIME_MINUTES);

                // Recalculate distance to delivery
                distanceToDelivery = currentPosition.distanceTo(deliveryPosition);
                fuelToDelivery = calculateFuelNeeded(distanceToDelivery, currentGlp, vehicle.getType());
            }

            // Now go to delivery
            currentFuel -= fuelToDelivery;
            currentPosition = deliveryPosition;
            currentTime = currentTime.plusMinutes((long) (distanceToDelivery / Constants.VEHICLE_AVG_SPEED * 60));

            stops.add(new RouteStop(currentPosition, order.getId(), order.getDeadlineTime(), part.getGlpDeliverM3()));
            currentTime = currentTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            currentGlp -= part.getGlpDeliverM3();
        }

        // Return to main depot
        Depot mainDepot = state.getMainDepot();
        double distanceToMainDepot = currentPosition.distanceTo(mainDepot.getPosition());
        double fuelToMainDepot = calculateFuelNeeded(distanceToMainDepot, currentGlp, vehicle.getType());

        if (currentFuel < fuelToMainDepot) {
            // Need fuel to reach main depot
            Depot nearestDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);
            if (nearestDepot == null) {
                return null;
            }

            double distanceToDepot = currentPosition.distanceTo(nearestDepot.getPosition());
            double fuelToDepot = calculateFuelNeeded(distanceToDepot, currentGlp, vehicle.getType());

            if (currentFuel < fuelToDepot) {
                return null;
            }

            // Go to depot
            currentFuel -= fuelToDepot;
            currentPosition = nearestDepot.getPosition();
            currentTime = currentTime.plusMinutes((long) (distanceToDepot / Constants.VEHICLE_AVG_SPEED * 60));

            stops.add(new RouteStop(currentPosition, nearestDepot.getId(), 0));
            currentFuel = maxFuel;
            currentTime = currentTime.plusMinutes(DEPOT_FUEL_REFILL_TIME_MINUTES);

            // Recalculate to main depot
            distanceToMainDepot = currentPosition.distanceTo(mainDepot.getPosition());
            fuelToMainDepot = calculateFuelNeeded(distanceToMainDepot, currentGlp, vehicle.getType());
        }

        // Go to main depot
        currentFuel -= fuelToMainDepot;
        currentPosition = mainDepot.getPosition();
        currentTime = currentTime.plusMinutes((long) (distanceToMainDepot / Constants.VEHICLE_AVG_SPEED * 60));
        stops.add(new RouteStop(currentPosition, mainDepot.getId(), 0));

        return new Route(vehicle.getId(), stops, startTime);
    }

    private static double calculateFuelNeeded(double distance, int currentGlp, VehicleType vehicleType) {
        double fuelConsumption = distance
                * (currentGlp * Constants.GLP_DENSITY_M3_TON + vehicleType.getTareWeightTon())
                * FUEL_SAFETY_FACTOR;
        return fuelConsumption / Constants.CONSUMPTION_FACTOR;
    }

    private static Depot findNearestDepot(Position position, int glpRequest, SimulationState state,
            Map<String, Integer> depotsGlpState) {
        Depot nearestDepot = null;
        double minDistance = Double.MAX_VALUE;

        List<Depot> allDepots = new ArrayList<>(state.getAuxDepots());
        allDepots.add(state.getMainDepot());

        for (Depot depot : allDepots) {
            if (depotsGlpState.get(depot.getId()) < glpRequest) {
                continue;
            }
            double distance = position.distanceTo(depot.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearestDepot = depot;
            }
        }

        return nearestDepot;
    }
}
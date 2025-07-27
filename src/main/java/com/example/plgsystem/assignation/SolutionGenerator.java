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
    private static final double SAFETY_FACTOR = 1.5; // Reduced from 1.5 to make routes more feasible
    private static final double FUEL_THRESHOLD = 0.4; // If fuel is below this ratio, consider visiting a depot

    public static Solution generateSolution(SimulationState state, Map<String, List<DeliveryPart>> assignments) {
        Map<String, Integer> depotsGlpState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();

        // Initialize depot GLP states
        for (Depot depot : state.getAuxDepots()) {
            depotsGlpState.put(depot.getId(), depot.getCurrentGlpM3());
        }
        depotsGlpState.put(state.getMainDepot().getId(), state.getMainDepot().getGlpCapacityM3());

        LocalDateTime startTime = state.getCurrentTime();
        for (Map.Entry<String, List<DeliveryPart>> entry : assignments.entrySet()) {
            String vehicleId = entry.getKey();
            List<DeliveryPart> deliveryParts = entry.getValue();

            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                continue;
            }

            // Sort deliveries by deadline for more efficient routes
            deliveryParts.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));

            Route route = buildRoute(vehicle, state, startTime, deliveryParts, depotsGlpState);
            routes.put(vehicleId, route);
        }

        return new Solution(routes, state);
    }

    public static Route buildRoute(Vehicle vehicle, SimulationState state, LocalDateTime startTime,
            List<DeliveryPart> deliveryParts, Map<String, Integer> depotsGlpState) {
        String mainDepotId = state.getMainDepot().getId();

        List<RouteStop> stops = new ArrayList<>();
        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        double maxFuel = vehicle.getFuelCapacityGal();

        LocalDateTime currentTime = vehicle.getCurrentActionEndTime() != null ? vehicle.getCurrentActionEndTime()
                : startTime;
        String vehicleId = vehicle.getId();
        LocalDateTime scheduledMaintenance = state.getMaintenanceSchedule().get(vehicleId);

        for (int i = 0; i < deliveryParts.size(); i++) {
            if (scheduledMaintenance != null && !scheduledMaintenance.isAfter(currentTime)) {
                Depot mainDepot = state.getMainDepot();
                if (!currentPosition.equals(mainDepot.getPosition())) {
                    double distanceToDepot = currentPosition.distanceTo(mainDepot.getPosition());
                    long travelTimeMinutes = Math.round((distanceToDepot / Constants.VEHICLE_AVG_SPEED) * 60);
                    currentTime = currentTime.plusMinutes(travelTimeMinutes);

                    currentPosition = mainDepot.getPosition();
                }

                stops.add(new RouteStop(currentPosition, mainDepot.getId(), currentTime));
                currentTime = currentTime.plusHours(Constants.MAINTENANCE_DURATION_HOURS);

                currentFuel = maxFuel;
                currentGlp = vehicle.getGlpCapacityM3();

                scheduledMaintenance = null;
            }

            if (currentFuel < FUEL_THRESHOLD * maxFuel) {
                Depot nearestDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);

                double distanceToDepot = currentPosition.distanceTo(nearestDepot.getPosition());
                long travelTimeMinutes = Math.round((distanceToDepot / Constants.VEHICLE_AVG_SPEED) * 60);
                if (nearestDepot.getId().equals(mainDepotId)) {
                    currentTime = currentTime
                            .plusMinutes(travelTimeMinutes + Constants.RELOAD_REFUEL_DURATION_MINUTES_MAIN_DEPOT);
                } else {
                    currentTime = currentTime.plusMinutes(travelTimeMinutes + Constants.RELOAD_REFUEL_DURATION_MINUTES);
                }

                currentFuel = maxFuel;
                currentPosition = nearestDepot.getPosition();
                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), 0));
            }

            DeliveryPart deliveryPart = deliveryParts.get(i);
            if (currentGlp < deliveryPart.getGlpDeliverM3()) {
                int glpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
                Depot nearestDepot = findNearestDepot(currentPosition, glpToLoad, state, depotsGlpState);

                // Calculate travel time to depot
                double distanceToDepot = currentPosition.distanceTo(nearestDepot.getPosition());
                long travelTimeMinutes = Math.round((distanceToDepot / Constants.VEHICLE_AVG_SPEED) * 60);
                if (nearestDepot.getId().equals(mainDepotId)) {
                    currentTime = currentTime
                            .plusMinutes(travelTimeMinutes + Constants.RELOAD_REFUEL_DURATION_MINUTES_MAIN_DEPOT);
                } else {
                    currentTime = currentTime.plusMinutes(travelTimeMinutes + Constants.RELOAD_REFUEL_DURATION_MINUTES);
                }
                // Update vehicle state
                currentFuel = maxFuel;
                currentGlp = vehicle.getGlpCapacityM3();
                currentPosition = nearestDepot.getPosition();
                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), glpToLoad));
            }

            Order order = state.getOrderById(deliveryPart.getOrderId());
            double distanceToOrder = currentPosition.distanceTo(order.getPosition());
            double fuelNeededToOrder = calculateFuelNeeded(distanceToOrder, currentGlp, vehicle.getType());

            long travelTimeMinutes = Math.round((distanceToOrder / Constants.VEHICLE_AVG_SPEED) * 60);
            currentTime = currentTime.plusMinutes(travelTimeMinutes);

            currentFuel -= fuelNeededToOrder;
            currentGlp -= deliveryPart.getGlpDeliverM3();
            currentPosition = order.getPosition();
            stops.add(new RouteStop(currentPosition, order.getId(), order.getDeadlineTime(),
                    deliveryPart.getGlpDeliverM3()));
            currentTime = currentTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        }

        Depot returnDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);
        int finalGlpToLoad = 0;
        if (returnDepot.getId() == state.getMainDepot().getId()) {
            returnDepot = state.getMainDepot();
            finalGlpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
        }
        RouteStop returnStop = new RouteStop(returnDepot.getPosition(), returnDepot.getId(), finalGlpToLoad);
        stops.add(returnStop);

        return new Route(vehicle.getId(), stops, startTime);
    }

    private static double calculateFuelNeeded(double distance, int currentGlp, VehicleType vehicleType) {
        double totalWeight = currentGlp * Constants.GLP_DENSITY_M3_TON + vehicleType.getTareWeightTon();
        return distance * totalWeight / Constants.CONSUMPTION_FACTOR * SAFETY_FACTOR;
    }

    private static Depot findNearestDepot(Position position, int glpRequest, SimulationState state,
            Map<String, Integer> depotsGlpState) {
        Depot nearestDepot = null;
        double minDistance = Double.MAX_VALUE;

        int currentHour = state.getCurrentTime().getHour();
        List<Depot> allDepots = new ArrayList<>();

        if (glpRequest > 0 && currentHour < 6) {
            return state.getMainDepot();
        }

        // If glpRequest is 0 (fuel only) or it's after 14:00, we can use any depot
        // Otherwise, apply time-based restrictions
        if (glpRequest == 0) {
            // For refueling only, add all depots
            for (Map.Entry<String, Integer> entry : depotsGlpState.entrySet()) {
                Depot depot = state.getDepotById(entry.getKey());
                if (depot != null) {
                    allDepots.add(depot);
                }
            }
        } else {
            for (Map.Entry<String, Integer> entry : depotsGlpState.entrySet()) {
                Depot depot = state.getDepotById(entry.getKey());
                if (depot == null)
                    continue;
                double remainingGlpPercentage = entry.getValue() / (double) depot.getGlpCapacityM3();
                // Between 10 AM and 2 PM, only use depots with more than 65% capacity
                if (currentHour < 12 && remainingGlpPercentage < 0.65) {
                    continue;
                } else if (currentHour < 18 && remainingGlpPercentage < 0.5) {
                    continue;
                }
                allDepots.add(depot);
            }
        }
        // Always ensure the main depot is included
        if (!allDepots.contains(state.getMainDepot())) {
            allDepots.add(state.getMainDepot());
        }

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
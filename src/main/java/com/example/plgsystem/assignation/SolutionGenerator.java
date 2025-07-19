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
    private static final double FUEL_THRESHOLD = 0.35; // If fuel is below this ratio, consider visiting a depot

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
                continue;
            }

            // Sort deliveries by deadline for more efficient routes
            deliveryParts.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));

            Route route = buildRoute(vehicle, state, startTime, deliveryParts, depotsGlpState);
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
        double maxFuel = vehicle.getFuelCapacityGal();

        for (int i = 0; i < deliveryParts.size(); i++) {
            // Check fuel level
            if (currentFuel < FUEL_THRESHOLD * maxFuel) {
                Depot nearestDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);
                // update vehicle
                currentFuel = maxFuel;
                currentPosition = nearestDepot.getPosition();
                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), 0));
            }

            // Check if we can reach the next delivery part
            DeliveryPart deliveryPart = deliveryParts.get(i);
            if (currentGlp < deliveryPart.getGlpDeliverM3()) {
                int glpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
                Depot nearestDepot = findNearestDepot(currentPosition, glpToLoad, state, depotsGlpState);
                // update vehicle
                currentFuel = maxFuel;
                currentGlp = vehicle.getGlpCapacityM3();
                currentPosition = nearestDepot.getPosition();
                stops.add(new RouteStop(currentPosition, nearestDepot.getId(), glpToLoad));
            }

            Order order = state.getOrderById(deliveryPart.getOrderId());
            double distanceToOrder = currentPosition.distanceTo(order.getPosition());
            double fuelNeededToOrder = calculateFuelNeeded(distanceToOrder, currentGlp, vehicle.getType());

            currentFuel -= fuelNeededToOrder;
            currentGlp -= deliveryPart.getGlpDeliverM3();
            currentPosition = order.getPosition();
            stops.add(new RouteStop(currentPosition, order.getId(), order.getDeadlineTime(), deliveryPart.getGlpDeliverM3()));
        }

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
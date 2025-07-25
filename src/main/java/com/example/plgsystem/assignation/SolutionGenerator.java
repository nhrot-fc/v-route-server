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

    private static Route buildRoute(Vehicle vehicle, SimulationState state, LocalDateTime startTime,
            List<DeliveryPart> deliveryParts, Map<String, Integer> depotsGlpState) {

        List<RouteStop> stops = new ArrayList<>();
        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        double maxFuel = vehicle.getFuelCapacityGal();

        LocalDateTime currentTime = vehicle.getCurrentActionEndTime() != null ? vehicle.getCurrentActionEndTime() : startTime;
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
                currentTime = currentTime.plusMinutes(travelTimeMinutes + Constants.REFUEL_DURATION_MINUTES);

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
                currentTime = currentTime.plusMinutes(travelTimeMinutes + Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);

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

        Depot returnDepot = state.getMainDepot();
        int finalGlpToLoad = Math.min(depotsGlpState.get(returnDepot.getId()), vehicle.getGlpCapacityM3() - currentGlp);
        if (returnDepot.getId() == state.getMainDepot().getId()) {
            returnDepot = state.getMainDepot();
            finalGlpToLoad = vehicle.getGlpCapacityM3() - currentGlp;
        }

        double distanceToDepot = currentPosition.distanceTo(returnDepot.getPosition());
        double fuelNeededToDepot = calculateFuelNeeded(distanceToDepot, currentGlp, vehicle.getType());

        if (currentFuel < fuelNeededToDepot) {
            Depot nearestDepot = findNearestDepot(currentPosition, 0, state, depotsGlpState);
            currentFuel = maxFuel;
            currentPosition = nearestDepot.getPosition();
            stops.add(new RouteStop(currentPosition, nearestDepot.getId(), 0));
            currentTime = currentTime.plusMinutes(Constants.REFUEL_DURATION_MINUTES);
        }
        currentPosition = returnDepot.getPosition();
        // Add maintenance stop if needed
        if (scheduledMaintenance != null && !scheduledMaintenance.isAfter(currentTime)) {
            stops.add(new RouteStop(currentPosition, returnDepot.getId(), scheduledMaintenance));
            currentTime = scheduledMaintenance.plusHours(Constants.MAINTENANCE_DURATION_HOURS);
        } else {
            stops.add(new RouteStop(currentPosition, returnDepot.getId(), finalGlpToLoad));
            currentTime = currentTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
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

        int currentHour = state.getCurrentTime().getHour();
        List<Depot> allDepots = new ArrayList<>();

        if (currentHour < 8) {
            // Solo el depósito principal de 00:00 a 07:59
            // allDepots ya está vacío
        } else if (currentHour < 12) {
            // De 08:00 a 11:59, solo los auxiliares con al menos 65% de GLP
            for (Depot depot : state.getAuxDepots()) {
                double porcentajeGLP = (double) depot.getCurrentGlpM3() / depot.getGlpCapacityM3();
                if (porcentajeGLP >= 0.65) {
                    allDepots.add(depot);
                }
            }
        } else if (currentHour < 14) {
            // De 12:00 a 13:59, solo los auxiliares con al menos 40% de GLP
            for (Depot depot : state.getAuxDepots()) {
                double porcentajeGLP = (double) depot.getCurrentGlpM3() / depot.getGlpCapacityM3();
                if (porcentajeGLP >= 0.4) {
                    allDepots.add(depot);
                }
            }
        } else {
            // De 14:00 en adelante, todos los auxiliares
            allDepots.addAll(state.getAuxDepots());
        }
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
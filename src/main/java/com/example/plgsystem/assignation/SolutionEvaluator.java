package com.example.plgsystem.assignation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class SolutionEvaluator {
    // Penalizaciones
    private static final double INCOMPLETE_ORDER_PENALTY = 5000.0;
    private static final double LATE_DELIVERY_PENALTY = 5000.0;
    private static final double DISTANCE_PENALTY_PER_KM = 0.01;

    private static final int DELIVER_SERVICE_TIME_MIN = Constants.GLP_SERVE_DURATION_MINUTES;
    private static final int LOAD_SERVICE_TIME_MIN = Constants.VEHICLE_GLP_TRANSFER_DURATION_MINUTES;

    public static Solution evaluate(Solution solution, SimulationState state) {
        Map<String, Integer> ordersState = new HashMap<>(solution.getOrdersState());
        Map<String, Integer> depotsState = new HashMap<>(solution.getDepotsState());
        Map<String, Route> routes = solution.getRoutes();

        double totalCost = 0;
        boolean isFeasible = true;

        for (String vehicleId : routes.keySet()) {
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                return createInfeasibleSolution(solution);
            }

            Route route = routes.get(vehicleId);
            double routeCost = evaluateRoute(route, vehicle, state, ordersState, depotsState);

            if (routeCost == Double.POSITIVE_INFINITY) {
                isFeasible = false;
                break;
            }

            totalCost += routeCost;
        }

        for (Map.Entry<String, Integer> entry : ordersState.entrySet()) {
            int remaining = entry.getValue();
            if (remaining > 0) {
                totalCost += remaining * INCOMPLETE_ORDER_PENALTY;
            }
        }

        if (!isFeasible) {
            return createInfeasibleSolution(solution);
        }

        return new Solution(ordersState, depotsState, routes, totalCost);
    }

    private static double evaluateRoute(Route route, Vehicle vehicle, SimulationState state,
            Map<String, Integer> ordersState, Map<String, Integer> depotsState) {
        List<RouteStop> stops = route.stops();
        if (stops.isEmpty()) {
            return 0.0;
        }

        double routeCost = 0.0;
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        Position currentPosition = vehicle.getCurrentPosition();
        LocalDateTime currentTime = route.startTime();

        for (RouteStop stop : stops) {
            Position targetPosition;

            if (stop.isOrderStop()) {
                Order order = state.getOrderById(stop.getOrderId());
                if (order == null) {
                    return Double.POSITIVE_INFINITY;
                }
                targetPosition = order.getPosition();
            } else {
                Depot depot = state.getDepotById(stop.getDepotId());
                if (depot == null) {
                    return Double.POSITIVE_INFINITY;
                }
                targetPosition = depot.getPosition();
            }

            List<Position> path = PathFinder.findPath(state, currentPosition, targetPosition, currentTime);
            if (path == null || path.isEmpty()) {
                return Double.POSITIVE_INFINITY; // No hay camino posible (error crítico)
            }

            double distance = path.size() - 1;

            routeCost += distance * DISTANCE_PENALTY_PER_KM;

            double fuelNeeded = vehicle.calculateFuelNeeded(distance);
            if (fuelNeeded > currentFuel) {
                return Double.POSITIVE_INFINITY; // No hay suficiente combustible (error crítico)
            }

            currentFuel -= fuelNeeded;

            Duration travelTime = Duration.ofSeconds(
                    (long) (distance / Constants.VEHICLE_AVG_SPEED * 3600));
            currentTime = currentTime.plus(travelTime);

            if (stop.isOrderStop() && stop.getOrderDeadlineTime() != null &&
                    currentTime.isAfter(stop.getOrderDeadlineTime())) {
                routeCost += LATE_DELIVERY_PENALTY;
            }

            if (stop.isOrderStop()) {
                int glpToDeliver = stop.getGlpDeliverM3();

                if (glpToDeliver > currentGlp) {
                    int glpDelivered = currentGlp;
                    currentGlp = 0;
                    String orderId = stop.getOrderId();
                    int remaining = ordersState.getOrDefault(orderId, 0) - glpDelivered;
                    ordersState.put(orderId, remaining);
                } else {
                    currentGlp -= glpToDeliver;

                    String orderId = stop.getOrderId();
                    int remaining = ordersState.getOrDefault(orderId, 0) - glpToDeliver;
                    ordersState.put(orderId, Math.max(0, remaining));
                }

                currentTime = currentTime.plusMinutes(DELIVER_SERVICE_TIME_MIN);
            } else {
                String depotId = stop.getDepotId();
                int glpToLoad = stop.getGlpLoadM3();

                Depot depot = state.getDepotById(depotId);
                if (!depot.isMain()) {
                    int depotGlp = depotsState.getOrDefault(depotId, 0);

                    if (depotGlp < glpToLoad) {

                        depotsState.put(depotId, 0);

                        int capacityRemaining = vehicle.getGlpCapacityM3() - currentGlp;
                        int actualLoad = Math.min(depotGlp, capacityRemaining);
                        currentGlp += actualLoad;

                        // Penalizamos la cantidad de GLP que no se pudo cargar
                        routeCost += (glpToLoad - depotGlp) * 5.0;
                    } else {
                        depotsState.put(depotId, depotGlp - glpToLoad);

                        int capacityRemaining = vehicle.getGlpCapacityM3() - currentGlp;
                        int actualLoad = Math.min(glpToLoad, capacityRemaining);
                        currentGlp += actualLoad;

                        if (actualLoad < glpToLoad) {
                            // Penalizamos la cantidad de GLP que no se pudo cargar
                            routeCost += (glpToLoad - actualLoad) * 2.0;
                        }
                    }
                } else {
                    int capacityRemaining = vehicle.getGlpCapacityM3() - currentGlp;
                    int actualLoad = Math.min(glpToLoad, capacityRemaining);
                    currentGlp += actualLoad;

                    if (actualLoad < glpToLoad) {
                        // Penalizamos la cantidad de GLP que no se pudo cargar
                        routeCost += (glpToLoad - actualLoad) * 2.0;
                    }
                }
                currentFuel = vehicle.getFuelCapacityGal();
                currentTime = currentTime.plusMinutes(LOAD_SERVICE_TIME_MIN);
            }
            currentPosition = targetPosition;
        }

        return routeCost;
    }

    /**
     * Crea una solución no factible con costo infinito
     */
    private static Solution createInfeasibleSolution(Solution originalSolution) {
        return new Solution(
                originalSolution.getOrdersState(),
                originalSolution.getDepotsState(),
                originalSolution.getRoutes(),
                Double.POSITIVE_INFINITY);
    }
}
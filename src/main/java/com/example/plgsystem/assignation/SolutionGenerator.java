package com.example.plgsystem.assignation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class SolutionGenerator {
    private static final double FUEL_SAFETY_FACTOR = 1.5;
    private static final double GLP_THRESHOLD_RATIO = 0.3;
    private static final double FUEL_THRESHOLD_RATIO = 0.3;

    public static Solution generateSolution(SimulationState state, Map<String, List<DeliveryPart>> assignments) {
        Map<String, Integer> depotsGlpState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();
        
        // Initialize depots GLP state
        initializeDepotsGlpState(state, depotsGlpState);
        
        LocalDateTime startTime = state.getCurrentTime();
        
        for (Map.Entry<String, List<DeliveryPart>> entry : assignments.entrySet()) {
            String vehicleId = entry.getKey();
            List<DeliveryPart> deliveryParts = entry.getValue();
            
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null || deliveryParts.isEmpty()) {
                continue;
            }
            
            // Build route with depot visits for this vehicle
            Route route = buildRouteWithDepotVisits(state, vehicle, deliveryParts, depotsGlpState, startTime);
            if (route != null && !route.stops().isEmpty()) {
                routes.put(vehicleId, route);
            }
        }
        
        return new Solution(routes, state);
    }

    private static void initializeDepotsGlpState(SimulationState state, Map<String, Integer> depotsGlpState) {
        // Add main depot
        depotsGlpState.put(state.getMainDepot().getId(), state.getMainDepot().getGlpCapacityM3());
        
        // Add auxiliary depots
        for (Depot depot : state.getAuxDepots()) {
            depotsGlpState.put(depot.getId(), depot.getGlpCapacityM3());
        }
    }
    
    private static Route buildRouteWithDepotVisits(
            SimulationState state, 
            Vehicle vehicle, 
            List<DeliveryPart> deliveryParts,
            Map<String, Integer> depotsGlpState, 
            LocalDateTime startTime) {
        
        Position currentPosition = vehicle.getCurrentPosition();
        int currentGlp = vehicle.getCurrentGlpM3();
        double currentFuel = vehicle.getCurrentFuelGal();
        LocalDateTime currentTime = startTime;
        
        List<RouteStop> stops = new ArrayList<>();
        
        for (DeliveryPart deliveryPart : deliveryParts) {
            Order order = state.getOrderById(deliveryPart.getOrderId());
            if (order == null) {
                continue;
            }
            
            Position orderPosition = order.getPosition();
            double distanceToOrder = currentPosition.distanceTo(orderPosition);
            
            // Check if we need to add a depot visit before this order
            RouteStop depotStop = checkAndAddDepotVisit(
                state, vehicle, currentPosition, currentGlp, currentFuel, 
                orderPosition, deliveryPart.getGlpDeliverM3(), depotsGlpState, currentTime);
            
            if (depotStop != null) {
                // Update position, glp, fuel, and time after depot visit
                currentPosition = depotStop.getPosition();
                currentGlp = vehicle.getGlpCapacityM3(); // Full GLP after depot visit
                currentFuel = vehicle.getFuelCapacityGal(); // Full fuel after depot visit
                
                // Time spent traveling to depot and refilling
                double distanceToDepot = depotStop.getDistanceFromPrevious();
                Duration travelTime = calculateTravelTime(distanceToDepot);
                Duration refillTime = Duration.ofMinutes(Constants.VEHICLE_GLP_TRANSFER_DURATION_MINUTES + Constants.REFUEL_DURATION_MINUTES);
                currentTime = currentTime.plus(travelTime).plus(refillTime);
                
                // Add depot stop to route
                stops.add(depotStop);
                
                // Recalculate distance to order from new position
                distanceToOrder = currentPosition.distanceTo(orderPosition);
            }
            
            // Check if this order location is blocked at estimated arrival time
            Duration travelTimeToOrder = calculateTravelTime(distanceToOrder);
            LocalDateTime estimatedArrivalTime = currentTime.plus(travelTimeToOrder);
            
            if (state.isPositionBlockedAt(orderPosition, estimatedArrivalTime)) {
                // Skip this order if location is blocked at arrival time
                continue;
            }
            
            // Calculate GLP to deliver based on order requirements and vehicle capacity
            int glpToDeliver = Math.min(deliveryPart.getGlpDeliverM3(), currentGlp);
            
            // Add order stop to route
            RouteStop orderStop = new RouteStop(
                    orderPosition, 
                    order.getId(), 
                    order.getDeadlineTime(), 
                    glpToDeliver,
                    distanceToOrder);
            stops.add(orderStop);
            
            // Update current state
            currentPosition = orderPosition;
            currentGlp -= glpToDeliver;
            currentFuel -= estimateFuelConsumption(vehicle, distanceToOrder);
            currentTime = estimatedArrivalTime.plus(Duration.ofMinutes(Constants.GLP_SERVE_DURATION_MINUTES));
        }
        
        // Add depot visit to main depot
        Depot mainDepot = state.getMainDepot();
        RouteStop depotStop = new RouteStop(mainDepot.getPosition(), mainDepot.getId(), 0, 0);
        stops.add(depotStop);

        return new Route(vehicle.getId(), stops, startTime);
    }
    
    private static RouteStop checkAndAddDepotVisit(
            SimulationState state, 
            Vehicle vehicle, 
            Position currentPosition, 
            int currentGlp, 
            double currentFuel, 
            Position nextDestination, 
            int requiredGlp,
            Map<String, Integer> depotsGlpState,
            LocalDateTime currentTime) {
        
        // Check if we need refill (GLP or fuel)
        boolean needsGlp = currentGlp < requiredGlp || needsRefill(vehicle, currentGlp);
        double distanceToDestination = currentPosition.distanceTo(nextDestination);
        double estimatedFuelNeeded = estimateFuelConsumption(vehicle, distanceToDestination);
        boolean needsFuel = needsRefuel(vehicle, currentFuel, estimatedFuelNeeded);
        
        if (!needsGlp && !needsFuel) {
            return null;
        }
        
        // Find nearest accessible depot with GLP
        Depot bestDepot = findBestAccessibleDepot(state, currentPosition, depotsGlpState, needsGlp, currentTime);
        if (bestDepot == null) {
            return null;
        }
        
        double distanceToDepot = currentPosition.distanceTo(bestDepot.getPosition());
        int glpToLoad = 0;
        
        // Calculate GLP to load if needed
        if (needsGlp) {
            int glpNeeded = vehicle.getGlpCapacityM3() - currentGlp;
            int availableGlp = depotsGlpState.get(bestDepot.getId());
            glpToLoad = Math.min(glpNeeded, availableGlp);
            
            // Update depot's GLP state
            depotsGlpState.put(bestDepot.getId(), availableGlp - glpToLoad);
        }
        
        // Create a depot stop with proper parameters
        RouteStop depotStop = new RouteStop(
                bestDepot.getPosition(), 
                bestDepot.getId(), 
                glpToLoad,
                distanceToDepot);
        
        return depotStop;
    }
    
    private static Depot findBestAccessibleDepot(
            SimulationState state, 
            Position currentPosition, 
            Map<String, Integer> depotsGlpState,
            boolean needsGlp,
            LocalDateTime currentTime) {
        
        Depot bestDepot = null;
        double minDistance = Double.MAX_VALUE;
        
        // Check main depot first
        Depot mainDepot = state.getMainDepot();
        double distanceToMain = currentPosition.distanceTo(mainDepot.getPosition());
        
        Duration travelTime = calculateTravelTime(distanceToMain);
        LocalDateTime estimatedArrivalTime = currentTime.plus(travelTime);
        
        if (!state.isPositionBlockedAt(mainDepot.getPosition(), estimatedArrivalTime) &&
            (!needsGlp || depotsGlpState.get(mainDepot.getId()) > 0)) {
            bestDepot = mainDepot;
            minDistance = distanceToMain;
        }
        
        // Check auxiliary depots
        for (Depot depot : state.getAuxDepots()) {
            double distance = currentPosition.distanceTo(depot.getPosition());
            
            if (distance >= minDistance) {
                continue;
            }
            
            travelTime = calculateTravelTime(distance);
            estimatedArrivalTime = currentTime.plus(travelTime);
            
            boolean isAccessible = !state.isPositionBlockedAt(depot.getPosition(), estimatedArrivalTime);
            boolean hasGlp = !needsGlp || depotsGlpState.get(depot.getId()) > 0;
            
            if (isAccessible && hasGlp) {
                bestDepot = depot;
                minDistance = distance;
            }
        }
        
        return bestDepot;
    }
    
    private static Duration calculateTravelTime(double distanceKm) {
        long secondsTaken = (long) (distanceKm * 3600 / Constants.VEHICLE_AVG_SPEED);
        return Duration.ofSeconds(secondsTaken);
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
}
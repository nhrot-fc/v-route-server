package com.example.plgsystem.simulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.VehiclePlan;

public class PlanExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PlanExecutor.class);
    

    public static void executePlan(SimulationState state, LocalDateTime nextTime){
        if (state.getCurrentVehiclePlans() == null || state.getCurrentVehiclePlans().isEmpty()) {
            logger.info("No vehicle plans to execute");
            return;
        }

        for (Map.Entry<String, VehiclePlan> entry : state.getCurrentVehiclePlans().entrySet()) {
            String vehicleId = entry.getKey();
            VehiclePlan plan = entry.getValue();
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                logger.error("Vehicle not found for plan: {}", plan);
                continue;
            }
            Action currentAction = plan.getCurrentAction();
            if (currentAction == null) {
                logger.error("No current action found for plan: {}", plan);
                continue;
            }
            while (currentAction.getStartTime().isBefore(nextTime) || currentAction.getStartTime().equals(nextTime)) {
                double progress = executeAction(state, currentAction, vehicle, nextTime);
                if (progress < 1.0) {
                    break;
                }
                vehicle.setAvailable();
                plan.advanceAction();
                currentAction = plan.getCurrentAction();
                if (currentAction == null) {
                    break;
                }
            }
        }
    }   

    private static double executeAction(SimulationState state, Action action, Vehicle vehicle, LocalDateTime nextTime) {
        double calculatedProgress = calculateProgress(action, nextTime);

        switch (action.getType()) {
            case REFUEL:
            case RELOAD:
            case SERVE:
                applyImmediateEffects(state, action, vehicle);
                break;
            case DRIVE:
            case MAINTENANCE:
            case WAIT:
                applyGradualEffects(action, vehicle, calculatedProgress);
                if (calculatedProgress >= 1.0) {
                    completeAction(state, action, vehicle);
                }
                break;
        }

        action.setCurrentProgress(calculatedProgress);
        return calculatedProgress;
    }

    private static double calculateProgress(Action action, LocalDateTime currentTime) {
        // If the current time is before the action start time, the progress is 0
        if (currentTime.isBefore(action.getStartTime())) {
            return 0.0;
        }
        
        // If the current time is after or equal to the end time, the action is complete
        if (currentTime.isAfter(action.getEndTime()) || currentTime.equals(action.getEndTime())) {
            return 1.0;
        }

        // Calculate the progress as a ratio of elapsed time to total duration
        long totalDurationMillis = Duration.between(action.getStartTime(), action.getEndTime()).toMillis();
        long elapsedMillis = Duration.between(action.getStartTime(), currentTime).toMillis();
        return Math.min(1.0, (double) elapsedMillis / totalDurationMillis);
    }

    private static void applyImmediateEffects(SimulationState state, Action action, Vehicle vehicle) {
        if (action.isEffectApplied()) {
            return;
        }

        // Apply immediate effects
        switch (action.getType()) {
            case REFUEL:
                vehicle.refuel();
                vehicle.setRefueling();
                break;
            case RELOAD:
                String depotId = action.getDepotId();
                Depot depot = state.getDepotById(depotId);
                if (depot == null) {
                    logger.error("Depot not found for action: {}", action);
                    return;
                }
                vehicle.refill(action.getGlpLoaded());
                depot.serve(action.getGlpLoaded());
                vehicle.setReloading();
                break;
            case SERVE:
                String orderId = action.getOrderId();
                Order order = state.getOrderById(orderId);
                if (order == null) {
                    logger.error("Order not found for action: {}", action);
                    return;
                }
                vehicle.serveOrder(order, action.getGlpDelivered(), action.getStartTime());
                vehicle.setServing();
                break;
            default:
                break;
        }
        action.setEffectApplied(true);
    }

    private static void applyGradualEffects(Action action, Vehicle vehicle, double progress) {
        double previousProgress = action.getCurrentProgress();
        
        switch (action.getType()) {
            case DRIVE:
                List<Position> path = action.getPath();
                if (path == null || path.isEmpty()) {
                    logger.error("No path found for action: {}", action);
                    return;
                }
                
                // Calculate total distance of the path
                double totalDistance = 0;
                for (int i = 0; i < path.size() - 1; i++) {
                    Position currentPosition = path.get(i);
                    Position nextPosition = path.get(i + 1);
                    totalDistance += currentPosition.distanceTo(nextPosition);
                }
                
                // Calculate how much distance should be traveled based on progress
                double distanceToTravel = totalDistance * progress;
                
                // Set vehicle status to driving
                vehicle.setDriving();
                
                // If not moving or already at destination, return
                if (progress <= 0) {
                    return;
                } else if (progress >= 1.0) {
                    // If action is complete, set position to final destination
                    vehicle.setCurrentPosition(path.get(path.size() - 1).clone());
                    // Consume fuel based on progress difference
                    vehicle.consumeFuel(action.getFuelConsumedGal() * (progress - previousProgress));
                    return;
                }
                
                // Find current position along the path
                double distanceTraveled = 0;
                for (int i = 0; i < path.size() - 1; i++) {
                    Position currentWaypoint = path.get(i);
                    Position nextWaypoint = path.get(i + 1);
                    double segmentDistance = currentWaypoint.distanceTo(nextWaypoint);
                    
                    if (distanceTraveled + segmentDistance >= distanceToTravel) {
                        // We found the segment where the vehicle currently is
                        double segmentProgress = (distanceToTravel - distanceTraveled) / segmentDistance;
                        double newX = currentWaypoint.getX() + segmentProgress * (nextWaypoint.getX() - currentWaypoint.getX());
                        double newY = currentWaypoint.getY() + segmentProgress * (nextWaypoint.getY() - currentWaypoint.getY());
                        
                        // Update vehicle position
                        vehicle.setCurrentPosition(new Position(newX, newY));
                        vehicle.consumeFuel(action.getFuelConsumedGal() * (progress - previousProgress));
                        return;
                    }
                    distanceTraveled += segmentDistance;
                }
                break;
                
            case MAINTENANCE:
                vehicle.setMaintenance();
                break;
                
            case WAIT:
                vehicle.setIdle();
                break;
                
            default:
                break;
        }
    }

    private static void completeAction(SimulationState state, Action action, Vehicle vehicle) {
        switch (action.getType()) {
            case DRIVE:
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    // Set the final position to the last point in the path
                    Position finalPosition = action.getPath().get(action.getPath().size() - 1);
                    vehicle.setCurrentPosition(finalPosition.clone());
                }
                break;
                
            case REFUEL:
                // Vehicle is fully refueled
                vehicle.refuel();
                vehicle.setAvailable(); // Set back to available
                break;
                
            case RELOAD:
                // GLP is already loaded in applyImmediateEffects
                vehicle.setAvailable(); // Set back to available
                break;
                
            case SERVE:
                // Order is already served in applyImmediateEffects
                vehicle.setAvailable(); // Set back to available
                break;
                
            case MAINTENANCE:
                vehicle.setAvailable(); // Set back to available
                break;
                
            case WAIT:
                vehicle.setAvailable(); // Set back to available
                break;
                
            default:
                break;
        }
    }
}

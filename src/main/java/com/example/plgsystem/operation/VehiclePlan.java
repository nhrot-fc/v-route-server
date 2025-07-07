package com.example.plgsystem.operation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.assignation.DeliveryPart;

/**
 * Represents a plan of actions for a vehicle to execute during the simulation.
 */
public class VehiclePlan {
    private final Vehicle vehicle;
    private final List<Action> actions;
    private final LocalDateTime planCreationTime;
    private final List<DeliveryPart> instructions;
    private final LocalDateTime startTime;
    private final List<Order> servedOrders;
    private final double totalDistanceKm;
    private final double totalGlpDeliveredM3;
    private final double totalFuelConsumedGal;

    public VehiclePlan(Vehicle vehicle, List<Action> actions, List<DeliveryPart> instructions) {
        this.vehicle = vehicle;
        this.actions = new ArrayList<>(actions);
        this.planCreationTime = LocalDateTime.now();
        this.instructions = instructions != null ? new ArrayList<>(instructions) : new ArrayList<>();

        List<Order> servedOrdersList = new ArrayList<>();
        double distKm = 0;
        int glpDelivered = 0;
        double fuelConsumed = 0;

        for (Action action : this.actions) {
            if (action.getType() == ActionType.SERVE && action.getOrder() != null) {
                servedOrdersList.add(action.getOrder());
                glpDelivered += Math.abs(action.getGlpChangeM3());
            }
            if (action.getType() == ActionType.DRIVE) {
                Position pathStart = action.getPath().getFirst();
                for (Position pos : action.getPath()) {
                    distKm += pathStart.distanceTo(pos);
                    pathStart = pos;
                }
                fuelConsumed += Math.abs(action.getFuelChangeGal());
            }
        }
        this.servedOrders = Collections.unmodifiableList(servedOrdersList);
        this.totalDistanceKm = distKm;
        this.totalGlpDeliveredM3 = glpDelivered;
        this.totalFuelConsumedGal = fuelConsumed;
        this.startTime = this.actions.isEmpty() ? null : this.actions.get(0).getExpectedStartTime();
    }

    /**
     * Gets the vehicle this plan is for
     * 
     * @return The vehicle
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Gets the list of actions in this plan
     * 
     * @return An unmodifiable list of actions
     */
    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    /**
     * Gets the creation time of this plan
     * 
     * @return The LocalDateTime when this plan was created
     */
    public LocalDateTime getPlanCreationTime() {
        return planCreationTime;
    }

    /**
     * Gets the delivery instructions this plan is based on
     * 
     * @return An unmodifiable list of delivery instructions
     */
    public List<DeliveryPart> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Gets the expected start time of the plan
     * 
     * @return The start time of the first action, or null if the plan is empty
     */
    public LocalDateTime getExpectedStartTime() {
        if (actions.isEmpty()) {
            return null;
        }
        return actions.get(0).getExpectedStartTime();
    }

    /**
     * Gets the expected end time of the plan
     * 
     * @return The end time of the last action, or null if the plan is empty
     */
    public LocalDateTime getExpectedEndTime() {
        if (actions.isEmpty()) {
            return null;
        }
        return actions.get(actions.size() - 1).getExpectedEndTime();
    }

    // Getters
    public List<Order> getServedOrders() {
        return Collections.unmodifiableList(servedOrders);
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getTotalGlpDeliveredM3() {
        return totalGlpDeliveredM3;
    }

    public double getTotalFuelConsumedGal() {
        return totalFuelConsumedGal;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Position getFinalPosition() {
        return actions.isEmpty() ? vehicle.getCurrentPosition() : actions.get(actions.size() - 1).getDestination();
    }

    public VehicleStatus getStatusAt(LocalDateTime time) {
        if (this.actions.isEmpty()) {
            return this.vehicle.getStatus();
        }

        if (time.isBefore(this.startTime)) {
            return this.vehicle.getStatus();
        }

        LocalDateTime currentActionStartTime = this.startTime;
        for (Action action : this.actions) {
            LocalDateTime currentActionEndTime = currentActionStartTime.plus(action.getDuration());

            if (!time.isBefore(currentActionStartTime) && time.isBefore(currentActionEndTime)) {
                switch (action.getType()) {
                    case DRIVE:
                        return VehicleStatus.DRIVING;
                    case REFUEL:
                        return VehicleStatus.REFUELING;
                    case RELOAD:
                        return VehicleStatus.RELOADING;
                    case SERVE:
                        return VehicleStatus.SERVING;
                    case MAINTENANCE:
                        return VehicleStatus.MAINTENANCE;
                    case WAIT:
                        return VehicleStatus.IDLE;
                    default:
                        return VehicleStatus.AVAILABLE;
                }
            }
            currentActionStartTime = currentActionEndTime; // Move to the start time of the next action
        }

        return VehicleStatus.AVAILABLE;
    }

    /**
     * Gets the action that should be executed at the given time
     * 
     * @param time The time to check
     * @return The action to execute at that time, or null if no action is scheduled
     */
    public Action getActionAt(LocalDateTime time) {
        if (this.actions.isEmpty() || time.isBefore(this.startTime)) {
            return null;
        }

        LocalDateTime currentActionStartTime = this.startTime;
        for (Action action : this.actions) {
            LocalDateTime currentActionEndTime = currentActionStartTime.plus(action.getDuration());

            if (!time.isBefore(currentActionStartTime) && time.isBefore(currentActionEndTime)) {
                return action;
            }
            currentActionStartTime = currentActionEndTime;
        }

        return null;
    }

    /**
     * Gets the start time for the given action
     * 
     * @param action The action to find
     * @return The start time of the action, or null if the action is not in the plan
     */
    public LocalDateTime getActionStartTime(Action action) {
        if (this.actions.isEmpty() || !this.actions.contains(action)) {
            return null;
        }

        LocalDateTime currentActionStartTime = this.startTime;
        for (Action currentAction : this.actions) {
            if (currentAction == action) {
                return currentActionStartTime;
            }
            currentActionStartTime = currentActionStartTime.plus(currentAction.getDuration());
        }

        return null;
    }

    /**
     * Gets the end time for the given action
     * 
     * @param action The action to find
     * @return The end time of the action, or null if the action is not in the plan
     */
    public LocalDateTime getActionEndTime(Action action) {
        LocalDateTime startTime = getActionStartTime(action);
        if (startTime == null) {
            return null;
        }
        return startTime.plus(action.getDuration());
    }
    
    /**
     * Gets the next action to execute at the given time.
     * 
     * @param currentTime The current time
     * @return The next action to execute, or null if no action is scheduled
     */
    public Action getNextAction(LocalDateTime currentTime) {
        LocalDateTime actionStartTime = this.startTime;
        for (Action action : this.actions) {
            LocalDateTime actionEndTime = actionStartTime.plus(action.getDuration());
            if (currentTime.isBefore(actionEndTime)) {
                return action;
            }
            
            actionStartTime = actionEndTime;
        }

        return null;
    }

    public int getOrderCount() {
        return servedOrders.size();
    }

    /**
     * Gets all path points from all driving actions in this plan
     * @return A list of all positions in the path
     */
    public List<Position> getPathPoints() {
        List<Position> allPoints = new ArrayList<>();
        
        // Add the starting position of the vehicle
        allPoints.add(vehicle.getCurrentPosition());
        
        // Add points from all driving actions
        for (Action action : actions) {
            if (action.getType() == ActionType.DRIVE && action.getPath() != null) {
                allPoints.addAll(action.getPath());
            }
        }
        
        return allPoints;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Vehicle Plan for %s (created at %s)\n", 
                vehicle.getId(), 
                planCreationTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));
        
        if (actions.isEmpty()) {
            sb.append("  No actions planned.\n");
        } else {
            for (int i = 0; i < actions.size(); i++) {
                sb.append(String.format("  [%d] %s\n", i + 1, actions.get(i)));
            }
            
            sb.append(String.format("  Total plan duration: %d minutes\n",
                    java.time.Duration.between(
                            getExpectedStartTime(), 
                            getExpectedEndTime()).toMinutes()));
        }
        
        return sb.toString();
    }
}

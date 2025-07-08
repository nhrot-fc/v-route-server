package com.example.plgsystem.operation;

import com.example.plgsystem.assignation.DeliveryPart;
import com.example.plgsystem.exceptions.InsufficientFuelException;
import com.example.plgsystem.exceptions.NoPathFoundException;
import com.example.plgsystem.model.*;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simplified Rules for Vehicle Plan Generation:
 * Start:
 * - If vehicle is at main depot → Execute routine maintenance
 * Core Logic (for each delivery):
 * 1. Check if vehicle needs GLP → Go to nearest GLP depot (handles fuel automatically)
 * 2. Go to order location (handles fuel automatically)
 * 3. Serve order
 * End:
 * - Return to main depot (handles fuel automatically)
 * Simple Go-To-Location Logic:
 * 1. Check if path exists → If not, return null
 * 2. Check if vehicle has enough fuel → If not, go to fuel depot first
 * 3. Go to location
 * Constraints:
 * - GLP can be refilled at any depot (main or auxiliary)
 * - Fuel can ONLY be refilled at the main depot
 * - Vehicle speed: Constant 50 Km/h
 * - Fuel consumption: (Distance * (TareWeight + GLPWeight)) / Constants.CONSUMPTION_FACTOR
 */
public class VehiclePlanCreator {

    private VehiclePlanCreator() {
        // Utility class with static methods only - prevent instantiation
    }

    public static LocalDateTime vehicleRefuel(Vehicle vehicle, Depot fuelDepot, LocalDateTime currentTime,
            List<Action> actions) {
        Action refuelAction = ActionFactory.createRefuelingAction(fuelDepot, vehicle, currentTime);
        actions.add(refuelAction);
        vehicle.refuel();
        return currentTime.plus(refuelAction.getDuration());
    }

    public static LocalDateTime vehicleRefill(Vehicle vehicle, Depot glpDepot, int glpAmountM3,
            LocalDateTime currentTime,
            List<Action> actions) {
        // Make sure we don't exceed the vehicle's GLP capacity
        int actualGlpAmount = Math.min(glpAmountM3, vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3());
        
        // Don't add an action if there's no GLP to add
        if (actualGlpAmount <= 0) {
            return currentTime;
        }
        
        Action refillAction = ActionFactory.createRefillingAction(glpDepot, actualGlpAmount, currentTime);
        actions.add(refillAction);
        vehicle.refill(actualGlpAmount);
        return currentTime.plus(refillAction.getDuration());
    }

    public static double calculatePathDistance(List<Position> path) {
        if (path == null || path.size() <= 1) {
            return 0.0;
        }
        return (path.size() - 1) * Constants.NODE_DISTANCE;
    }

    /**
     * Checks if there's a valid path to destination
     */
    public static boolean hasPath(SimulationState environment, Position from, Position to, LocalDateTime currentTime) {
        if (from.equals(to)) {
            return true;
        }
        List<Position> path = PathFinder.findPath(environment, from, to, currentTime);
        return !path.isEmpty();
    }

    /**
     * Checks if vehicle has enough fuel to reach destination
     */
    public static boolean hasEnoughFuel(SimulationState environment, Vehicle vehicle, Position destination, LocalDateTime currentTime) {
        if (vehicle.getCurrentPosition().equals(destination)) {
            return true;
        }

        List<Position> path = PathFinder.findPath(environment, vehicle.getCurrentPosition(), destination, currentTime);
        if (path.isEmpty()) {
            return false; // No path available
        }

        double distanceKm = calculatePathDistance(path);
        double fuelConsumedGal = vehicle.calculateFuelNeeded(distanceKm);
        return vehicle.getCurrentFuelGal() > fuelConsumedGal + Constants.EPSILON;
    }

    /**
     * Simple drive method - assumes path and fuel checks are done beforehand
     */
    public static LocalDateTime driveToLocation(SimulationState environment, Vehicle vehicle, Position destination,
            LocalDateTime currentTime, List<Action> actions) throws NoPathFoundException, InsufficientFuelException {
        if (vehicle.getCurrentPosition().equals(destination)) {
            return currentTime;
        }

        List<Position> path = PathFinder.findPath(environment, vehicle.getCurrentPosition(), destination, currentTime);
        if (path.isEmpty()) {
            throw new NoPathFoundException("No path found from " + vehicle.getCurrentPosition() + " to " + destination);
        }

        double distanceKm = calculatePathDistance(path);
        double fuelConsumedGal = vehicle.calculateFuelNeeded(distanceKm);

        if (fuelConsumedGal > vehicle.getCurrentFuelGal()) {
            throw new InsufficientFuelException("Not enough fuel to reach destination. Need " +
                    fuelConsumedGal + " gal, but only have " + vehicle.getCurrentFuelGal() + " gal.");
        }

        Duration duration = Duration.ofMinutes((int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60.0));

        Action drivingAction = ActionFactory.createDrivingAction(path, fuelConsumedGal, currentTime,
                currentTime.plus(duration));
        actions.add(drivingAction);

        vehicle.setCurrentPosition(destination);
        vehicle.consumeFuel(distanceKm);

        return currentTime.plus(duration);
    }

    /**
     * Tries to go to a location with the simple logic:
     * 1. Check if path exists
     * 2. Check if vehicle has enough fuel - if not, go to fuel depot first
     * 3. Go to location
     */
    public static LocalDateTime goToLocation(SimulationState environment, Vehicle vehicle, Position destination,
            LocalDateTime currentTime, List<Action> actions) {
        try {
            // 1. Check if path exists
            if (!hasPath(environment, vehicle.getCurrentPosition(), destination, currentTime)) {
                return null; // No path available
            }

            // 2. Check if vehicle has enough fuel
            if (!hasEnoughFuel(environment, vehicle, destination, currentTime)) {
                // Need to refuel first
                Depot fuelDepot = environment.getMainDepot();
                
                // Check if vehicle can reach fuel depot
                if (!hasPath(environment, vehicle.getCurrentPosition(), fuelDepot.getPosition(), currentTime) ||
                    !hasEnoughFuel(environment, vehicle, fuelDepot.getPosition(), currentTime)) {
                    return null; // Can't reach fuel depot
                }
                
                // Go to fuel depot and refuel
                currentTime = driveToLocation(environment, vehicle, fuelDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefuel(vehicle, fuelDepot, currentTime, actions);
                
                // Check again if vehicle can reach destination after refueling
                if (!hasPath(environment, vehicle.getCurrentPosition(), destination, currentTime) ||
                    !hasEnoughFuel(environment, vehicle, destination, currentTime)) {
                    return null; // Still can't reach destination
                }
            }

            // 3. Go to location
            return driveToLocation(environment, vehicle, destination, currentTime, actions);
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to go to location: " + e.getMessage());
            return null;
        }
    }

    public static LocalDateTime processFuelSupply(SimulationState environment, Vehicle vehicle, LocalDateTime currentTime,
            List<Action> actions) {
        try {
            // Fuel can ONLY be obtained at the main depot
            Depot mainDepot = environment.getMainDepot();
            if (!hasPath(environment, vehicle.getCurrentPosition(), mainDepot.getPosition(), currentTime) ||
                !hasEnoughFuel(environment, vehicle, mainDepot.getPosition(), currentTime)) {
                return null;
            }

            LocalDateTime updatedTime = driveToLocation(environment, vehicle, mainDepot.getPosition(), currentTime, actions);
            updatedTime = vehicleRefuel(vehicle, mainDepot, updatedTime, actions);

            return updatedTime;
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to refuel: " + e.getMessage());
            return null;
        }
    }

    public static LocalDateTime processGlpSupply(SimulationState environment, Vehicle vehicle, int glpRequired,
            LocalDateTime currentTime, List<Action> actions) {
        try {
            // Find the closest depot with sufficient GLP
            Depot glpDepot = findNearestGLPDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(), glpRequired,
                    environment.getMainDepot());

            // Use the new simple logic to go to GLP depot
            currentTime = goToLocation(environment, vehicle, glpDepot.getPosition(), currentTime, actions);
            if (currentTime == null) {
                return null; // Couldn't reach GLP depot
            }

            // Refill GLP
            currentTime = vehicleRefill(vehicle, glpDepot, glpRequired, currentTime, actions);

            // If at main depot, also refuel
            if (glpDepot == environment.getMainDepot()) {
                currentTime = vehicleRefuel(vehicle, glpDepot, currentTime, actions);
            }

            return currentTime;
        } catch (Exception e) {
            System.err.println("Failed to complete GLP supply: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a vehicle plan based on a list of delivery instructions
     * 
     * @param environment The simulation environment
     * @param vehicle The vehicle to create a plan for
     * @param instructions The delivery instructions to include in the plan
     * @return A vehicle plan, or null if the plan could not be created
     */
    public static VehiclePlan createPlan(SimulationState environment, Vehicle vehicle, List<DeliveryPart> instructions) {
        if (vehicle == null || instructions == null || instructions.isEmpty()) {
            return null;
        }
        
        // For now, create a placeholder plan
        // In a real implementation, this would calculate paths, times, fuel consumption, etc.
        List<Action> actions = new ArrayList<>();
        
        // TODO: Implement the full vehicle plan creation based on delivery instructions
        // This would involve:
        // 1. Finding paths between all locations
        // 2. Calculating travel times, fuel consumption
        // 3. Adding loading/unloading actions
        // 4. Adding refueling and maintenance actions as needed
        
        return new VehiclePlan(vehicle, actions, instructions);
    }
    
    /**
     * Creates a default plan for a vehicle to return to the main depot
     * 
     * @param environment The simulation environment
     * @param vehicle The vehicle to create a plan for
     * @return A vehicle plan to return to the main depot, or null if the plan could not be created
     */
    public static VehiclePlan createPlanToMainDepot(SimulationState environment, Vehicle vehicle) {
        if (vehicle == null || environment == null) {
            return null;
        }
        
        Depot mainDepot = environment.getMainDepot();
        if (mainDepot == null) {
            return null;
        }
        
        // For now, create a placeholder plan
        // In a real implementation, this would calculate path, time, fuel consumption, etc.
        List<Action> actions = new ArrayList<>();
        
        // TODO: Implement the default plan creation to return to depot
        // This would involve:
        // 1. Finding a path to the main depot
        // 2. Calculating travel time and fuel consumption
        // 3. Adding any needed refueling actions
        
        return new VehiclePlan(vehicle, actions, null);
    }

    public static Depot findNearestGLPDepot(List<Depot> depots, Position currentPosition, int glpNeeded,
            Depot mainDepot) {
        List<Depot> availableDepots = new ArrayList<>(depots);
        availableDepots.add(mainDepot);
        return availableDepots.stream()
                .filter(depot -> depot.getCurrentGlpM3() >= glpNeeded)
                .min(Comparator.comparingDouble(d -> currentPosition.distanceTo(d.getPosition())))
                .orElse(mainDepot);
    }
}
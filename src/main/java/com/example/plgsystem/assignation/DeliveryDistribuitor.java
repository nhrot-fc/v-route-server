package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class DeliveryDistribuitor {

    private static final int PACKAGE_SIZE = 5;
    private final SimulationState environment;
    private final Random random = new Random();

    public DeliveryDistribuitor(SimulationState environment) {
        this.environment = environment;
    }

    public Solution createInitialRandomAssignments() {
        Map<Vehicle, List<DeliveryInstruction>> assignments = new HashMap<>();
        List<Vehicle> availableVehicles = environment.getAvailableVehicles();
        List<Order> pendingOrders = new ArrayList<>(environment.getPendingOrders());

        // If there are no pending orders or vehicles, return an empty solution
        if (pendingOrders.isEmpty() || availableVehicles.isEmpty()) {
            System.err.println("Warning: No pending orders or available vehicles for assignment.");
            // Initialize empty lists for available vehicles
            for (Vehicle vehicle : availableVehicles) {
                assignments.put(vehicle, new ArrayList<>());
            }
            return new Solution(assignments);
        }

        // Initialize assignment lists for each vehicle
        for (Vehicle vehicle : availableVehicles) {
            assignments.put(vehicle, new ArrayList<>());
        }

        // Create mini packages of 5 GLP units from each order
        List<DeliveryInstruction> allPackages = new ArrayList<>();
        for (Order order : pendingOrders) {
            int remainingGlpToAssign = order.getRemainingGlpM3();
            
            // Skip if order has no remaining GLP
            if (remainingGlpToAssign <= 0) {
                continue;
            }
            
            // Create packages of PACKAGE_SIZE (5) or smaller for the last package
            while (remainingGlpToAssign > 0) {
                int packageSize = Math.min(remainingGlpToAssign, PACKAGE_SIZE);
                allPackages.add(new DeliveryInstruction(order.clone(), packageSize));
                remainingGlpToAssign -= packageSize;
            }
        }
        
        // Distribute packages using weighted random selection
        for (DeliveryInstruction instruction : allPackages) {
            Vehicle selectedVehicle = selectVehicleByCapacityWeight(availableVehicles);
            assignments.get(selectedVehicle).add(instruction);
        }

        return new Solution(assignments);
    }

    /**
     * Select a vehicle with bias towards those with larger capacity
     * Vehicles with more GLP capacity have a higher probability of being selected
     */
    private Vehicle selectVehicleByCapacityWeight(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("No vehicles available for selection");
        }
        
        // Calculate total capacity for normalization
        int totalCapacity = 0;
        for (Vehicle vehicle : vehicles) {
            totalCapacity += vehicle.getGlpCapacityM3();
        }
        
        // Generate a random value between 0 and total capacity
        double randomValue = random.nextDouble() * totalCapacity;
        
        // Select vehicle based on weighted probability
        double cumulativeWeight = 0;
        for (Vehicle vehicle : vehicles) {
            cumulativeWeight += vehicle.getGlpCapacityM3();
            if (randomValue <= cumulativeWeight) {
                return vehicle;
            }
        }
        
        // Fallback to the last vehicle (should not happen with proper implementation)
        return vehicles.get(vehicles.size() - 1);
    }
}

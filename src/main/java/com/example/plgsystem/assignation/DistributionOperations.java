package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class DistributionOperations {
    private static final Random random = new Random();

    /**
     * Creates a deep copy of the original assignments
     */
    public static Map<String, List<DeliveryPart>> cloneAssignments(Map<String, List<DeliveryPart>> original) {
        Map<String, List<DeliveryPart>> clone = new HashMap<>();
        for (String vehicleId : original.keySet()) {
            clone.put(vehicleId, new ArrayList<>(original.get(vehicleId)));
        }
        return clone;
    }

    /**
     * Reintegrates lost delivery parts into other vehicles.
     * Use this when some delivery parts couldn't be assigned due to route constraints.
     */
    public static Map<String, List<DeliveryPart>> reintegrateDeliveryParts(
            Map<String, List<DeliveryPart>> assignments, 
            List<DeliveryPart> lostParts, 
            SimulationState state) {
        if (lostParts.isEmpty()) {
            return assignments;
        }

        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);
        
        // Sort lost parts by deadline (most urgent first)
        lostParts.sort(DeliveryPart::compareTo);

        // Get vehicles that have valid routes
        List<String> viableVehicles = new ArrayList<>(result.keySet());
        
        if (viableVehicles.isEmpty()) {
            return result; // No viable vehicles to assign to
        }
        
        // For each lost part, try to assign it to a vehicle
        for (DeliveryPart part : lostParts) {
            // Select vehicle with weighting by capacity
            List<Vehicle> vehicles = new ArrayList<>();
            for (String vehicleId : viableVehicles) {
                Vehicle vehicle = state.getVehicleById(vehicleId);
                if (vehicle != null) {
                    vehicles.add(vehicle);
                }
            }
            
            if (!vehicles.isEmpty()) {
                Vehicle selectedVehicle = selectVehicleByCapacityWeight(vehicles);
                result.get(selectedVehicle.getId()).add(part);
            }
        }
        
        return result;
    }

    /**
     * Select a vehicle with bias towards those with larger capacity.
     */
    private static Vehicle selectVehicleByCapacityWeight(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("No vehicles available for selection");
        }
        int totalCapacity = 0;
        for (Vehicle vehicle : vehicles) {
            totalCapacity += vehicle.getGlpCapacityM3();
        }
        double randomValue = random.nextDouble() * totalCapacity;
        double cumulativeWeight = 0;
        for (Vehicle vehicle : vehicles) {
            cumulativeWeight += vehicle.getGlpCapacityM3();
            if (randomValue <= cumulativeWeight) {
                return vehicle;
            }
        }
        return vehicles.getLast();
    }

    /**
     * Operation 1: Shuffle a random segment of a vehicle's deliveries with optimization
     */
    public static Map<String, List<DeliveryPart>> shuffleSegment(Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with non-empty assignments
        List<String> vehiclesWithAssignments = result.entrySet().stream()
                .filter(e -> e.getValue().size() > 2) // Need at least 3 items to shuffle segment
                .map(Map.Entry::getKey)
                .toList();

        if (vehiclesWithAssignments.isEmpty()) {
            return result; // No eligible vehicles
        }

        // Select random vehicle
        String vehicleId = vehiclesWithAssignments.get(random.nextInt(vehiclesWithAssignments.size()));
        List<DeliveryPart> deliveries = result.get(vehicleId);

        // Select random segment
        int size = deliveries.size();
        int start = random.nextInt(size - 1); // Ensure there's at least 2 elements
        int end = start + 1 + random.nextInt(size - start - 1) + 1; // At least start+1, at most size

        // Create sublist and shuffle it
        List<DeliveryPart> segment = new ArrayList<>(deliveries.subList(start, end));
        Collections.shuffle(segment);

        // Replace segment in original list
        for (int i = start; i < end; i++) {
            deliveries.set(i, segment.get(i - start));
        }

        // Apply optimization if state is provided
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }
        return result;
    }

    /**
     * Operation 2: Sort deliveries by deadline for a random vehicle with optimization
     */
    public static Map<String, List<DeliveryPart>> sortByDeadline(Map<String, List<DeliveryPart>> assignments, 
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with multiple deliveries
        List<String> eligibleVehicles = result.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (eligibleVehicles.isEmpty()) {
            return result;
        }

        // Option 1: Sort a single random vehicle
        if (random.nextDouble() < 0.5) {
            // Select random vehicle
            String vehicleId = eligibleVehicles.get(random.nextInt(eligibleVehicles.size()));
            List<DeliveryPart> deliveries = result.get(vehicleId);
            
            // Sort using natural ordering (DeliveryPart.compareTo)
            Collections.sort(deliveries);
        } 
        // Option 2: Sort all vehicles
        else {
            for (String vehicleId : eligibleVehicles) {
                List<DeliveryPart> deliveries = result.get(vehicleId);
                deliveries.sort(DeliveryPart::compareTo);
            }
        }

        // Apply optimization if state is provided
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }
        return result;
    }

    /**
     * Operation 3: Balance GLP distribution by vehicle capacity
     */
    public static Map<String, List<DeliveryPart>> balanceByCapacity(Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Calculate total GLP and capacity for each vehicle
        Map<String, Integer> glpPerVehicle = new HashMap<>();
        Map<String, Integer> capacityPerVehicle = new HashMap<>();

        for (String vehicleId : result.keySet()) {
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null)
                continue;

            int totalGlp = result.get(vehicleId).stream()
                    .mapToInt(DeliveryPart::getGlpDeliverM3)
                    .sum();

            glpPerVehicle.put(vehicleId, totalGlp);
            capacityPerVehicle.put(vehicleId, vehicle.getGlpCapacityM3());
        }

        // Find overloaded and underloaded vehicles based on capacity ratio
        List<String> overloadedVehicles = new ArrayList<>();
        List<String> underloadedVehicles = new ArrayList<>();

        for (String vehicleId : result.keySet()) {
            if (!capacityPerVehicle.containsKey(vehicleId))
                continue;

            int glp = glpPerVehicle.getOrDefault(vehicleId, 0);
            int capacity = capacityPerVehicle.get(vehicleId);
            double ratio = (double) glp / capacity;

            // Define overloaded and underloaded based on ratio to average
            if (ratio > 0.7 && !result.get(vehicleId).isEmpty()) {
                overloadedVehicles.add(vehicleId);
            } else if (ratio < 0.3) {
                underloadedVehicles.add(vehicleId);
            }
        }

        // Balance by moving deliveries from overloaded to underloaded
        if (!overloadedVehicles.isEmpty() && !underloadedVehicles.isEmpty()) {
            // Number of moves to make - more aggressive balancing
            int movesToMake = 1 + random.nextInt(2); // 1-2 moves
            
            for (int move = 0; move < movesToMake; move++) {
                // Re-evaluate after each move
                if (overloadedVehicles.isEmpty() || underloadedVehicles.isEmpty()) {
                    break;
                }
                
                // Pick random overloaded and underloaded vehicles
                String sourceVehicleId = overloadedVehicles.get(random.nextInt(overloadedVehicles.size()));
                String targetVehicleId = underloadedVehicles.get(random.nextInt(underloadedVehicles.size()));

                List<DeliveryPart> sourceDeliveries = result.get(sourceVehicleId);
                List<DeliveryPart> targetDeliveries = result.get(targetVehicleId);

                // Move a random delivery
                if (!sourceDeliveries.isEmpty()) {
                    int sourcePos = random.nextInt(sourceDeliveries.size());
                    DeliveryPart deliveryToMove = sourceDeliveries.remove(sourcePos);
                    targetDeliveries.add(deliveryToMove);
                    
                    // Update GLP counts
                    int movedGlp = deliveryToMove.getGlpDeliverM3();
                    glpPerVehicle.put(sourceVehicleId, glpPerVehicle.get(sourceVehicleId) - movedGlp);
                    glpPerVehicle.put(targetVehicleId, glpPerVehicle.get(targetVehicleId) + movedGlp);
                    
                    // Re-evaluate vehicles
                    double sourceRatio = (double) glpPerVehicle.get(sourceVehicleId) / capacityPerVehicle.get(sourceVehicleId);
                    double targetRatio = (double) glpPerVehicle.get(targetVehicleId) / capacityPerVehicle.get(targetVehicleId);
                    
                    if (sourceRatio <= 0.7) {
                        overloadedVehicles.remove(sourceVehicleId);
                    }
                    
                    if (targetRatio >= 0.3) {
                        underloadedVehicles.remove(targetVehicleId);
                    }
                }
            }
        }

        // Always optimize after balancing
        return DeliveryOptimizer.optimizeAssignments(result, state);
    }

    /**
     * Operation 4: Move a delivery from one vehicle to another with optimization
     */
    public static Map<String, List<DeliveryPart>> moveDeliveryBetweenVehicles(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get source vehicles (with deliveries)
        List<String> sourceVehicles = result.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        if (sourceVehicles.isEmpty()) {
            return result; // No source vehicles
        }

        // Get all vehicles as potential targets
        List<String> allVehicles = new ArrayList<>(result.keySet());

        if (allVehicles.size() < 2) {
            return result; // Need at least 2 vehicles
        }

        // Select random source vehicle
        String sourceVehicleId = sourceVehicles.get(random.nextInt(sourceVehicles.size()));

        // Select random target vehicle (different from source)
        String targetVehicleId;
        do {
            targetVehicleId = allVehicles.get(random.nextInt(allVehicles.size()));
        } while (targetVehicleId.equals(sourceVehicleId));

        // Get delivery lists
        List<DeliveryPart> sourceDeliveries = result.get(sourceVehicleId);
        List<DeliveryPart> targetDeliveries = result.get(targetVehicleId);

        // Select random delivery from source
        int sourcePos = random.nextInt(sourceDeliveries.size());
        DeliveryPart deliveryToMove = sourceDeliveries.remove(sourcePos);

        // Add to target
        targetDeliveries.add(deliveryToMove);

        // Apply optimization if state is provided
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }
        return result;
    }

    /**
     * Operation 5: Swap all deliveries between two vehicles with optimization
     */
    public static Map<String, List<DeliveryPart>> swapVehicles(Map<String, List<DeliveryPart>> assignments, 
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Need at least 2 vehicles
        if (result.size() < 2) {
            return result;
        }

        // Select two different random vehicles
        List<String> vehicles = new ArrayList<>(result.keySet());
        int index1 = random.nextInt(vehicles.size());
        int index2;
        do {
            index2 = random.nextInt(vehicles.size());
        } while (index2 == index1);

        String vehicleId1 = vehicles.get(index1);
        String vehicleId2 = vehicles.get(index2);

        // Swap the entire lists
        List<DeliveryPart> temp = result.get(vehicleId1);
        result.put(vehicleId1, result.get(vehicleId2));
        result.put(vehicleId2, temp);

        // Apply optimization if state is provided
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }
        return result;
    }

    /**
     * Performs a random operation on the assignments, using only the simplified operations
     */
    public static Map<String, List<DeliveryPart>> randomOperationWithState(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        int operationType = random.nextInt(5);

        // Perform the operation with state to ensure optimization happens
        return switch (operationType) {
            case 0 -> shuffleSegment(assignments, state);
            case 1 -> sortByDeadline(assignments, state);
            case 2 -> balanceByCapacity(assignments, state);
            case 3 -> moveDeliveryBetweenVehicles(assignments, state);
            case 4 -> swapVehicles(assignments, state);
            default -> DeliveryOptimizer.optimizeAssignments(cloneAssignments(assignments), state);
        };
    }
    
    /**
     * Performs operations prioritizing balancing and deadline ordering.
     * Used for periodic rebalancing to escape local optima.
     */
    public static Map<String, List<DeliveryPart>> performStrategicRebalancing(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        // First, sort all routes by deadline
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);
        
        // Sort all vehicles' routes using natural ordering (DeliveryPart.compareTo)
        for (List<DeliveryPart> deliveries : result.values()) {
            if (deliveries.size() > 1) {
                Collections.sort(deliveries);
            }
        }
        
        // Then perform thorough balancing - multiple moves to reach better balance
        result = aggressiveBalancing(result, state);
        
        return result;
    }
    
    /**
     * Performs multiple balance operations to achieve a better global balance
     */
    private static Map<String, List<DeliveryPart>> aggressiveBalancing(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);
        
        // Calculate initial capacities and loads
        Map<String, Integer> glpPerVehicle = new HashMap<>();
        Map<String, Integer> capacityPerVehicle = new HashMap<>();
        
        for (String vehicleId : result.keySet()) {
            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) continue;
            
            int totalGlp = result.get(vehicleId).stream()
                    .mapToInt(DeliveryPart::getGlpDeliverM3)
                    .sum();
                    
            glpPerVehicle.put(vehicleId, totalGlp);
            capacityPerVehicle.put(vehicleId, vehicle.getGlpCapacityM3());
        }
        
        // Calculate global load ratio
        int totalGlp = glpPerVehicle.values().stream().mapToInt(Integer::intValue).sum();
        int totalCapacity = capacityPerVehicle.values().stream().mapToInt(Integer::intValue).sum();
        double globalRatio = (double) totalGlp / totalCapacity;
        
        // Perform multiple moves for better balance
        int maxMoves = result.size() * 2; // Arbitrary number based on fleet size
        int movesMade = 0;
        
        while (movesMade < maxMoves) {
            // Find most overloaded and most underloaded vehicles
            String mostOverloaded = null;
            String mostUnderloaded = null;
            double maxOverRatio = 0;
            double minUnderRatio = Double.MAX_VALUE;
            
            for (String vehicleId : result.keySet()) {
                if (!capacityPerVehicle.containsKey(vehicleId)) continue;
                
                int glp = glpPerVehicle.getOrDefault(vehicleId, 0);
                int capacity = capacityPerVehicle.get(vehicleId);
                double ratio = (double) glp / capacity;
                
                // Compare to global ratio
                if (ratio > globalRatio * 1.2 && ratio > maxOverRatio && !result.get(vehicleId).isEmpty()) {
                    maxOverRatio = ratio;
                    mostOverloaded = vehicleId;
                }
                
                if (ratio < globalRatio * 0.8 && ratio < minUnderRatio) {
                    minUnderRatio = ratio;
                    mostUnderloaded = vehicleId;
                }
            }
            
            // If can't find suitable vehicles, stop balancing
            if (mostOverloaded == null || mostUnderloaded == null || mostOverloaded.equals(mostUnderloaded)) {
                break;
            }
            
            // Move a delivery from overloaded to underloaded
            List<DeliveryPart> sourceDeliveries = result.get(mostOverloaded);
            List<DeliveryPart> targetDeliveries = result.get(mostUnderloaded);
            
            // Sort by GLP amount and move largest one that fits
            sourceDeliveries.sort(Comparator.comparing(DeliveryPart::getGlpDeliverM3).reversed());
            
            boolean moveMade = false;
            for (int i = 0; i < sourceDeliveries.size(); i++) {
                DeliveryPart part = sourceDeliveries.get(i);
                int glpAmount = part.getGlpDeliverM3();
                
                // Check if this move would improve balance
                double newSourceRatio = (double)(glpPerVehicle.get(mostOverloaded) - glpAmount) / 
                        capacityPerVehicle.get(mostOverloaded);
                double newTargetRatio = (double)(glpPerVehicle.get(mostUnderloaded) + glpAmount) / 
                        capacityPerVehicle.get(mostUnderloaded);
                
                // If the move improves balance (gets both closer to global ratio)
                if (Math.abs(newSourceRatio - globalRatio) < Math.abs(maxOverRatio - globalRatio) &&
                    Math.abs(newTargetRatio - globalRatio) < Math.abs(minUnderRatio - globalRatio)) {
                    
                    // Remove from source
                    sourceDeliveries.remove(i);
                    // Add to target
                    targetDeliveries.add(part);
                    
                    // Update GLP counts
                    glpPerVehicle.put(mostOverloaded, glpPerVehicle.get(mostOverloaded) - glpAmount);
                    glpPerVehicle.put(mostUnderloaded, glpPerVehicle.get(mostUnderloaded) + glpAmount);
                    
                    moveMade = true;
                    break;
                }
            }
            
            // If no suitable move was found, stop balancing
            if (!moveMade) {
                break;
            }
            
            movesMade++;
        }
        
        return result;
    }
}

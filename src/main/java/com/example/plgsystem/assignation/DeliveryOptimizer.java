package com.example.plgsystem.assignation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

/**
 * Utility class to optimize delivery assignments by applying split and merge
 * operations
 */
public class DeliveryOptimizer {

    /**
     * Atomic size for delivery units - the minimum size that a delivery can be
     * divided into
     */
    private static final int ATOMIC_CHUNK_SIZE = 1;

    /**
     * Post-processes assignments after operations by applying:
     * 1. splitConsecutiveAndAtomic on all deliveries
     * 2. mergeLikeRLE on all vehicle routes based on their capacity
     * 
     * @param assignments Current assignments to optimize
     * @param state       SimulationState containing vehicle capacity information
     * @return Optimized assignments map
     */
    public static Map<String, List<DeliveryPart>> optimizeAssignments(
            Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {

        // Create new result map
        Map<String, List<DeliveryPart>> result = new HashMap<>();

        // For each vehicle:
        for (String vehicleId : assignments.keySet()) {
            List<DeliveryPart> deliveries = assignments.get(vehicleId);

            // 1. Split into atomic chunks
            List<DeliveryPart> atomicParts = DeliveryConsolidator.splitConsecutiveAndAtomic(
                    deliveries, ATOMIC_CHUNK_SIZE);

            // 2. Get vehicle capacity from state
            Vehicle vehicle = state.getVehicleById(vehicleId);
            int capacity = (vehicle != null) ? vehicle.getGlpCapacityM3() : 20; // Default capacity if vehicle not found

            List<DeliveryPart> mergedParts = DeliveryConsolidator.mergeLikeRLE(atomicParts, capacity);

            // 4. Store in result
            result.put(vehicleId, mergedParts);
        }

        return result;
    }

    /**
     * Move a delivery part from one vehicle to another with proper
     * splitting/merging
     * 
     * @param assignments         Current assignments map
     * @param sourceVehicleId     Vehicle ID to move delivery from
     * @param targetVehicleId     Vehicle ID to move delivery to
     * @param deliveryIndexToMove Index of the delivery to move in the source
     *                            vehicle's list
     * @param state               SimulationState for vehicle capacities
     * @return Updated assignments with optimized routes
     */
    public static Map<String, List<DeliveryPart>> moveDeliveryWithOptimization(
            Map<String, List<DeliveryPart>> assignments,
            String sourceVehicleId,
            String targetVehicleId,
            int deliveryIndexToMove,
            SimulationState state) {

        // Create a deep copy to avoid modifying the original
        Map<String, List<DeliveryPart>> result = DistributionOperations.cloneAssignments(assignments);

        // Ensure both vehicles exist in assignments
        if (!result.containsKey(sourceVehicleId) || !result.containsKey(targetVehicleId) ||
                result.get(sourceVehicleId).size() <= deliveryIndexToMove) {
            return result; // Return unchanged if invalid parameters
        }

        // Get delivery lists
        List<DeliveryPart> sourceDeliveries = result.get(sourceVehicleId);
        List<DeliveryPart> targetDeliveries = result.get(targetVehicleId);

        // Remove the delivery from source
        DeliveryPart deliveryToMove = sourceDeliveries.remove(deliveryIndexToMove);

        // Add to target
        targetDeliveries.add(deliveryToMove);

        // Apply optimization to the entire solution
        return optimizeAssignments(result, state);
    }
}

package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
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
     * Internal operation: Shuffle a random segment of a vehicle's deliveries
     */
    public static Map<String, List<DeliveryPart>> shuffleSegment(Map<String, List<DeliveryPart>> assignments) {
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

        return result;
    }

    /**
     * Internal operation: Move a delivery from one position to another within the
     * same vehicle
     */
    public static Map<String, List<DeliveryPart>> moveDelivery(Map<String, List<DeliveryPart>> assignments) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with non-empty assignments
        List<String> vehiclesWithAssignments = result.entrySet().stream()
                .filter(e -> e.getValue().size() > 1) // Need at least 2 items to move
                .map(Map.Entry::getKey)
                .toList();

        if (vehiclesWithAssignments.isEmpty()) {
            return result; // No eligible vehicles
        }

        // Select random vehicle
        String vehicleId = vehiclesWithAssignments.get(random.nextInt(vehiclesWithAssignments.size()));
        List<DeliveryPart> deliveries = result.get(vehicleId);

        // Select random source and target positions
        int size = deliveries.size();
        int sourcePos = random.nextInt(size);
        int targetPos;
        do {
            targetPos = random.nextInt(size);
        } while (targetPos == sourcePos);

        // Move the delivery
        DeliveryPart movedDelivery = deliveries.remove(sourcePos);
        deliveries.add(targetPos, movedDelivery);

        return result;
    }

    /**
     * External operation: Swap one delivery between two vehicles
     */
    public static Map<String, List<DeliveryPart>> swapDeliveries(Map<String, List<DeliveryPart>> assignments) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with non-empty assignments
        List<String> vehiclesWithAssignments = result.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        if (vehiclesWithAssignments.size() < 2) {
            return result; // Need at least 2 vehicles
        }

        // Select two different random vehicles
        int index1 = random.nextInt(vehiclesWithAssignments.size());
        int index2;
        do {
            index2 = random.nextInt(vehiclesWithAssignments.size());
        } while (index2 == index1);

        String vehicleId1 = vehiclesWithAssignments.get(index1);
        String vehicleId2 = vehiclesWithAssignments.get(index2);

        List<DeliveryPart> deliveries1 = result.get(vehicleId1);
        List<DeliveryPart> deliveries2 = result.get(vehicleId2);

        // If either list is empty, return unchanged
        if (deliveries1.isEmpty() || deliveries2.isEmpty()) {
            return result;
        }

        // Select random delivery from each vehicle
        int pos1 = random.nextInt(deliveries1.size());
        int pos2 = random.nextInt(deliveries2.size());

        // Swap the deliveries
        DeliveryPart temp = deliveries1.get(pos1);
        deliveries1.set(pos1, deliveries2.get(pos2));
        deliveries2.set(pos2, temp);

        return result;
    }

    /**
     * External operation: Move a delivery from one vehicle to another
     */
    public static Map<String, List<DeliveryPart>> moveDeliveryBetweenVehicles(
            Map<String, List<DeliveryPart>> assignments) {
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

        return result;
    }

    /**
     * External operation: Swap all deliveries between two vehicles
     */
    public static Map<String, List<DeliveryPart>> swapVehicles(Map<String, List<DeliveryPart>> assignments) {
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

        return result;
    }

    /**
     * Internal operation: Sort deliveries by deadline for a random vehicle
     */
    public static Map<String, List<DeliveryPart>> sortByDeadline(Map<String, List<DeliveryPart>> assignments) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with multiple deliveries
        List<String> eligibleVehicles = result.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (eligibleVehicles.isEmpty()) {
            return result;
        }

        // Select random vehicle
        String vehicleId = eligibleVehicles.get(random.nextInt(eligibleVehicles.size()));
        List<DeliveryPart> deliveries = result.get(vehicleId);

        // Sort by deadline
        deliveries.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));

        return result;
    }

    /**
     * Internal operation: Reverse the order of deliveries for a random vehicle
     */
    public static Map<String, List<DeliveryPart>> reverseDeliveries(Map<String, List<DeliveryPart>> assignments) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Get vehicles with multiple deliveries
        List<String> eligibleVehicles = result.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (eligibleVehicles.isEmpty()) {
            return result;
        }

        // Select random vehicle
        String vehicleId = eligibleVehicles.get(random.nextInt(eligibleVehicles.size()));
        List<DeliveryPart> deliveries = result.get(vehicleId);

        // Reverse order
        Collections.reverse(deliveries);

        return result;
    }

    /**
     * External operation: Balance GLP distribution by vehicle capacity
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
            }
        }

        return result;
    }

    /**
     * External operation: Geographic clustering - move deliveries between vehicles
     * based on proximity
     */
    public static Map<String, List<DeliveryPart>> geographicClustering(Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Need at least 2 vehicles with deliveries
        List<String> vehiclesWithDeliveries = result.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        if (vehiclesWithDeliveries.size() < 2) {
            return result;
        }

        // Select two random vehicles
        String vehicleId1 = vehiclesWithDeliveries.get(random.nextInt(vehiclesWithDeliveries.size()));
        String vehicleId2;
        do {
            vehicleId2 = vehiclesWithDeliveries.get(random.nextInt(vehiclesWithDeliveries.size()));
        } while (vehicleId2.equals(vehicleId1));

        // Get positions of orders for both vehicles
        Map<String, Position> orderPositions = new HashMap<>();
        for (String orderId : result.get(vehicleId1).stream().map(DeliveryPart::getOrderId)
                .toList()) {
            Order order = state.getOrderById(orderId);
            if (order != null) {
                orderPositions.put(orderId, order.getPosition());
            }
        }
        for (String orderId : result.get(vehicleId2).stream().map(DeliveryPart::getOrderId)
                .toList()) {
            Order order = state.getOrderById(orderId);
            if (order != null) {
                orderPositions.put(orderId, order.getPosition());
            }
        }

        // Determine centers for both vehicles
        Position center1 = calculateCenter(result.get(vehicleId1), orderPositions);
        Position center2 = calculateCenter(result.get(vehicleId2), orderPositions);

        if (center1 == null || center2 == null) {
            return result;
        }

        // Find deliveries from vehicle1 closer to center2
        List<DeliveryPart> deliveriesToMove1to2 = new ArrayList<>();
        for (DeliveryPart delivery : result.get(vehicleId1)) {
            Position pos = orderPositions.get(delivery.getOrderId());
            if (pos != null) {
                if (pos.distanceTo(center2) < pos.distanceTo(center1)) {
                    deliveriesToMove1to2.add(delivery);
                }
            }
        }

        // Find deliveries from vehicle2 closer to center1
        List<DeliveryPart> deliveriesToMove2to1 = new ArrayList<>();
        for (DeliveryPart delivery : result.get(vehicleId2)) {
            Position pos = orderPositions.get(delivery.getOrderId());
            if (pos != null) {
                if (pos.distanceTo(center1) < pos.distanceTo(center2)) {
                    deliveriesToMove2to1.add(delivery);
                }
            }
        }

        // Move at most one delivery in each direction
        if (!deliveriesToMove1to2.isEmpty()) {
            DeliveryPart delivery = deliveriesToMove1to2.get(random.nextInt(deliveriesToMove1to2.size()));
            result.get(vehicleId1).remove(delivery);
            result.get(vehicleId2).add(delivery);
        }

        if (!deliveriesToMove2to1.isEmpty()) {
            DeliveryPart delivery = deliveriesToMove2to1.get(random.nextInt(deliveriesToMove2to1.size()));
            result.get(vehicleId2).remove(delivery);
            result.get(vehicleId1).add(delivery);
        }

        return result;
    }

    /**
     * External operation: Balance delivery count among vehicles
     */
    public static Map<String, List<DeliveryPart>> balanceDeliveryCount(Map<String, List<DeliveryPart>> assignments) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Find vehicle with most and least deliveries
        String maxVehicleId = null;
        String minVehicleId = null;
        int maxCount = -1;
        int minCount = Integer.MAX_VALUE;

        for (Map.Entry<String, List<DeliveryPart>> entry : result.entrySet()) {
            int count = entry.getValue().size();
            if (count > maxCount) {
                maxCount = count;
                maxVehicleId = entry.getKey();
            }
            if (count < minCount) {
                minCount = count;
                minVehicleId = entry.getKey();
            }
        }

        // If there's a significant imbalance, move a delivery
        if (maxVehicleId != null && minVehicleId != null && !maxVehicleId.equals(minVehicleId)
                && maxCount > minCount + 1) {
            List<DeliveryPart> maxDeliveries = result.get(maxVehicleId);
            List<DeliveryPart> minDeliveries = result.get(minVehicleId);

            // Move a random delivery from max to min
            int sourcePos = random.nextInt(maxDeliveries.size());
            DeliveryPart deliveryToMove = maxDeliveries.remove(sourcePos);
            minDeliveries.add(deliveryToMove);
        }

        return result;
    }

    /**
     * Helper method to calculate the center position of a set of deliveries
     */
    private static Position calculateCenter(List<DeliveryPart> deliveries, Map<String, Position> positionMap) {
        if (deliveries.isEmpty())
            return null;

        int totalX = 0;
        int totalY = 0;
        int count = 0;

        for (DeliveryPart delivery : deliveries) {
            Position pos = positionMap.get(delivery.getOrderId());
            if (pos != null) {
                totalX += pos.getX();
                totalY += pos.getY();
                count++;
            }
        }

        if (count == 0)
            return null;

        return new Position(totalX / count, totalY / count);
    }

    /**
     * Performs a random operation on the assignments, including operations that
     * require state information
     */
    public static Map<String, List<DeliveryPart>> randomOperationWithState(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        int operationType = random.nextInt(10);

        return switch (operationType) {
            case 0 -> shuffleSegment(assignments);
            case 1 -> moveDelivery(assignments);
            case 2 -> swapDeliveries(assignments);
            case 3 -> moveDeliveryBetweenVehicles(assignments);
            case 4 -> swapVehicles(assignments);
            case 5 -> sortByDeadline(assignments);
            case 6 -> reverseDeliveries(assignments);
            case 7 -> balanceByCapacity(assignments, state);
            case 8 -> geographicClustering(assignments, state);
            case 9 -> balanceDeliveryCount(assignments);
            default -> cloneAssignments(assignments);
        };
    }
}

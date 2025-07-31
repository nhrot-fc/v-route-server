package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class DistributionOperations {
    private static final Random random = new Random();

    public static Map<String, List<DeliveryPart>> cloneAssignments(Map<String, List<DeliveryPart>> original) {
        Map<String, List<DeliveryPart>> clone = new HashMap<>();
        for (String vehicleId : original.keySet()) {
            clone.put(vehicleId, new ArrayList<>(original.get(vehicleId)));
        }
        return clone;
    }

    /*
     * ======================================================
     * EXPLOITATION OPERATIONS
     * ======================================================
     */

    public static Map<String, List<DeliveryPart>> sortDeliveries(Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        for (String vehicleId : result.keySet()) {
            List<DeliveryPart> deliveries = result.get(vehicleId);

            // Sort deliveries by deadline
            if (deliveries.isEmpty() || deliveries.size() == 1) {
                continue; // Skip empty lists
            }
            deliveries.sort(DeliveryPart::compareTo);
            result.put(vehicleId, deliveries);
        }
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }

        return result;
    }

    public static Map<String, List<DeliveryPart>> greedySort(Map<String, List<DeliveryPart>> assignments,
            SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        for (String vehicleId : result.keySet()) {
            List<DeliveryPart> deliveries = result.get(vehicleId);

            // Skip empty lists or single delivery (already optimized)
            if (deliveries.isEmpty() || deliveries.size() == 1) {
                continue;
            }

            Vehicle vehicle = state.getVehicleById(vehicleId);
            if (vehicle == null) {
                continue; // Skip if vehicle not found
            }

            // First, consolidate by order ID and divide by vehicle capacity
            deliveries.sort(DeliveryPart::compareTo);
            deliveries = DeliveryConsolidator.mergeLikeRLE(deliveries, vehicle.getGlpCapacityM3());

            // Heurística: "Elige el destino viable más urgente"
            // 1. Para cada destino, evaluar si es posible llegar a tiempo
            // 2. De los viables, elegir el de deadline más cercano
            // 3. En caso de empate, elegir el más cercano por distancia

            Position currentPosition = vehicle.getCurrentPosition();
            List<DeliveryPart> optimizedDeliveries = new ArrayList<>();
            double currentTime = 0; // Tiempo relativo para la simulación

            while (!deliveries.isEmpty()) {
                List<ViableDelivery> viableDeliveries = new ArrayList<>();

                // Identificar destinos viables (aquellos a los que podemos llegar a tiempo)
                for (DeliveryPart delivery : deliveries) {
                    Order order = state.getOrderById(delivery.getOrderId());
                    if (order == null)
                        continue;

                    Position orderPosition = order.getPosition();
                    double distance = currentPosition.distanceTo(orderPosition);
                    double travelTime = distance / Constants.VEHICLE_AVG_SPEED;
                    double arrivalTime = currentTime + travelTime;

                    // Convertimos a horas para hacer la comparación con el deadline
                    LocalDateTime simulatedArrivalTime = state.getCurrentTime().plusMinutes((long) (arrivalTime * 60));

                    // Solo consideramos viable si podemos llegar antes del deadline
                    if (!simulatedArrivalTime.isAfter(delivery.getDeadlineTime())) {
                        viableDeliveries.add(new ViableDelivery(delivery, order, distance, arrivalTime));
                    }
                }

                // Si no hay entregas viables, terminamos
                if (viableDeliveries.isEmpty()) {
                    break;
                }

                // Ordenar por deadline (criterio primario) y por distancia (criterio
                // secundario)
                viableDeliveries.sort(Comparator
                        .comparing((ViableDelivery v) -> v.delivery.getDeadlineTime())
                        .thenComparing(v -> v.distance));

                // Elegir la mejor opción (la más urgente y cercana)
                ViableDelivery bestChoice = viableDeliveries.get(0);

                // Añadir a la ruta optimizada y eliminar de pendientes
                optimizedDeliveries.add(bestChoice.delivery);
                deliveries.remove(bestChoice.delivery);

                // Actualizar posición y tiempo para la siguiente iteración
                currentPosition = bestChoice.order.getPosition();
                currentTime = bestChoice.arrivalTime;

                // Añadir tiempo de servicio (en horas)
                currentTime += Constants.GLP_SERVE_DURATION_MINUTES / 60.0;
            }

            // Actualizar la lista de entregas con la versión optimizada
            result.put(vehicleId, optimizedDeliveries);
        }

        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }

        return result;
    }

    private static class ViableDelivery {
        final DeliveryPart delivery;
        final Order order;
        final double distance;
        final double arrivalTime;

        ViableDelivery(DeliveryPart delivery, Order order, double distance, double arrivalTime) {
            this.delivery = delivery;
            this.order = order;
            this.distance = distance;
            this.arrivalTime = arrivalTime;
        }
    }

    /*
     * =======================================================
     * EXPLORATION OPERATIONS
     * =======================================================
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
                    double sourceRatio = (double) glpPerVehicle.get(sourceVehicleId)
                            / capacityPerVehicle.get(sourceVehicleId);
                    double targetRatio = (double) glpPerVehicle.get(targetVehicleId)
                            / capacityPerVehicle.get(targetVehicleId);

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

    public static Map<String, List<DeliveryPart>> shuffleOrderAssignments(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);
        if (state == null) {
            return result; // No state means no optimization
        }
        // from state extract X number of Orders
        List<Order> orders = state.getOrders();
        int randomCount = Math.min(orders.size(), 5 + random.nextInt(6)); // 5-10 orders
        Collections.shuffle(orders);
        List<Order> selectedOrders = orders.subList(0, randomCount);

        // for each order remove from assignments
        for (Order order : selectedOrders) {
            String orderId = order.getId();
            for (String vehicleId : result.keySet()) {
                List<DeliveryPart> deliveries = result.get(vehicleId);
                deliveries.removeIf(dp -> dp.getOrderId().equals(orderId));
            }
        }

        // Now reassign these orders randomly to vehicles
        for (Order order : selectedOrders) {
            String orderId = order.getId();
            DeliveryPart newDelivery = new DeliveryPart(orderId, order.getRemainingGlpM3(), order.getDeadlineTime());

            // Assign to a random vehicle
            List<String> vehicleIds = new ArrayList<>(result.keySet());
            String randomVehicleId = vehicleIds.get(random.nextInt(vehicleIds.size()));
            result.get(randomVehicleId).add(newDelivery);
        }

        return DeliveryOptimizer.optimizeAssignments(result, state);
    }

    public static Map<String, List<DeliveryPart>> shuffleDeliveryAssigments(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        List<DeliveryPart> extractedDeliveries = new ArrayList<>();
        for (Map.Entry<String, List<DeliveryPart>> entry : result.entrySet()) {
            List<DeliveryPart> deliveries = entry.getValue();
            if (deliveries.isEmpty()) {
                continue;
            }
            // Shuffle deliveries and extract a random number
            Collections.shuffle(deliveries);
            int randomCount = 1 + random.nextInt(deliveries.size());
            extractedDeliveries.addAll(deliveries.subList(0, randomCount));
            // Remove these deliveries from the original vehicle
            deliveries.subList(0, randomCount).clear();
            result.put(entry.getKey(), deliveries);
        }

        // Now reassign these extracted deliveries randomly to vehicles
        for (DeliveryPart delivery : extractedDeliveries) {
            // Assign to a random vehicle
            List<String> vehicleIds = new ArrayList<>(result.keySet());
            String randomVehicleId = vehicleIds.get(random.nextInt(vehicleIds.size()));
            result.get(randomVehicleId).add(delivery);
        }

        return DeliveryOptimizer.optimizeAssignments(result, state);
    }

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

    public static Map<String, List<DeliveryPart>> shuffleSegments(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        Map<String, List<DeliveryPart>> result = cloneAssignments(assignments);

        // Shuffle each vehicle's deliveries
        for (String vehicleId : result.keySet()) {
            List<DeliveryPart> deliveries = result.get(vehicleId);
            if (deliveries.isEmpty()) {
                continue; // Skip empty lists
            }
            double randomFactor = random.nextDouble();
            if (randomFactor > 0.7) {
                continue;
            }
            int randomLeft = random.nextInt(deliveries.size());
            int randomRight = randomLeft + random.nextInt(deliveries.size() - randomLeft);

            // Extract the segment and shuffle it
            List<DeliveryPart> segment = deliveries.subList(randomLeft, randomRight);
            Collections.shuffle(segment);
            // Replace the original segment with the shuffled one
            for (int i = 0; i < segment.size(); i++) {
                deliveries.set(randomLeft + i, segment.get(i));
            }

            result.put(vehicleId, deliveries);
        }

        // Optimize if state is provided
        if (state != null) {
            return DeliveryOptimizer.optimizeAssignments(result, state);
        }
        return result;
    }

    public static Map<String, List<DeliveryPart>> randomOperationWithState(
            Map<String, List<DeliveryPart>> assignments, SimulationState state) {
        int operationType = random.nextInt(5);

        // Perform the operation with state to ensure optimization happens
        return switch (operationType) {
            case 0 -> balanceByCapacity(assignments, state);
            case 1 -> shuffleOrderAssignments(assignments, state);
            case 2 -> shuffleDeliveryAssigments(assignments, state);
            case 3 -> swapVehicles(assignments, state);
            case 4 -> shuffleSegments(assignments, state);
            default -> DeliveryOptimizer.optimizeAssignments(cloneAssignments(assignments), state);
        };
    }
}
package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

public class RandomDistributor {
    private static final int PACKAGE_SIZE = 5;
    private static final Random random = new Random();

    public static Map<String, List<DeliveryPart>> createInitialRandomAssignments(SimulationState environment) {
        Map<String, List<DeliveryPart>> assignments = new HashMap<>();
        List<Vehicle> availableVehicles = environment.getVehicles().stream()
                .filter(Vehicle::isAvailable).toList();
        List<Order> pendingOrders = environment.getOrders();

        for (Vehicle vehicle : availableVehicles) {
            assignments.put(vehicle.getId(), new ArrayList<>());
        }

        if (pendingOrders.isEmpty() || availableVehicles.isEmpty()) {
            return assignments;
        }

        List<DeliveryPart> allPackages = new ArrayList<>();
        for (Order order : pendingOrders) {
            int remainingGlpToAssign = order.getRemainingGlpM3();

            if (remainingGlpToAssign <= 0) {
                continue;
            }

            while (remainingGlpToAssign > 0) {
                int packageSize = Math.min(remainingGlpToAssign, PACKAGE_SIZE);
                allPackages.add(new DeliveryPart(order.getId(), packageSize, order.getDeadlineTime()));
                remainingGlpToAssign -= packageSize;
            }
        }

        allPackages.sort(Comparator.comparing(DeliveryPart::getDeadlineTime));
        for (DeliveryPart deliveryPart : allPackages) {
            Vehicle selectedVehicle = selectVehicleByCapacityWeight(availableVehicles);
            assignments.get(selectedVehicle.getId()).add(deliveryPart);
        }

        return assignments;
    }

    /**
     * Select a vehicle with bias towards those with larger capacity. Vehicles with
     * more GLP capacity have a higher probability of being selected
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
}

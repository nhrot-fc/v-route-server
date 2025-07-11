package com.example.plgsystem.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.assignation.DeliveryPart;
import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.RandomDistributor;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.operation.VehiclePlanCreator;
import com.example.plgsystem.simulation.SimulationState;

/**
 * Demo application to visualize vehicle planning in detail.
 * Run this class directly to see all the messages with complete details.
 */
public class VehiclePlanDemo {

    public static void main(String[] args) {
        System.out.println("=== Vehicle Plan Demo ===\n");

        // Setup the test environment
        LocalDateTime currentTime = LocalDateTime.now();
        List<Vehicle> vehicles = createTestVehicles();
        Depot mainDepot = createMainDepot();
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Order> orders = createTestOrders(currentTime);

        System.out.println("1. Initializing simulation state...");
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, currentTime);
        for (Order order : orders) {
            state.addOrder(order);
        }

        System.out.println("State:");
        System.out.println(state);

        System.out.println("\n2. Creating initial random assignments...");
        Map<String, List<DeliveryPart>> routes = RandomDistributor.createInitialRandomAssignments(state);
        printDeliveryParts(routes);

        System.out.println("\n3. Solving with metaheuristic solver...");
        Solution solution = MetaheuristicSolver.solve(state);
        System.out.println("\nFull solution:");
        System.out.println(solution);

        System.out.println("\n4. Creating vehicle plans from routes...");
        Map<String, VehiclePlan> vehiclePlans = new HashMap<>();
        for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            VehiclePlan vehiclePlan = VehiclePlanCreator.createPlanFromRoute(route, state, currentTime);
            vehiclePlans.put(vehicleId, vehiclePlan);
        }

        System.out.println("\nVehicle Plans:");
        for (Map.Entry<String, VehiclePlan> entry : vehiclePlans.entrySet()) {
            System.out.println("\nPlan for vehicle " + entry.getKey() + ":");
            System.out.println("----------------------------------");
            System.out.println(entry.getValue());
        }
    }

    private static void printDeliveryParts(Map<String, List<DeliveryPart>> routes) {
        for (Map.Entry<String, List<DeliveryPart>> entry : routes.entrySet()) {
            String vehicleId = entry.getKey();
            List<DeliveryPart> deliveryParts = entry.getValue();
            System.out.println("Vehicle " + vehicleId + ": ");
            for (DeliveryPart deliveryPart : deliveryParts) {
                System.out.println("\t" + deliveryPart.toString());
            }
        }
    }

    private static List<Vehicle> createTestVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        // Vehículo tipo TA
        Vehicle vehicleTA = new Vehicle(
                "TA-01",
                VehicleType.TA,
                new Position(10, 10)); // Posición cerca del centro
        vehicles.add(vehicleTA);

        // Vehículo tipo TB
        Vehicle vehicleTB = new Vehicle(
                "TB-01",
                VehicleType.TB,
                new Position(40, 40)); // Posición cerca del depósito norte
        vehicles.add(vehicleTB);

        // Vehículo tipo TC
        Vehicle vehicleTC = new Vehicle(
                "TC-01",
                VehicleType.TC,
                new Position(60, 5)); // Posición cerca del depósito este
        vehicles.add(vehicleTC);

        // Vehículo tipo TD
        Vehicle vehicleTD = new Vehicle(
                "TD-01",
                VehicleType.TD,
                new Position(35, 25)); // Posición central
        vehicles.add(vehicleTD);

        return vehicles;
    }

    private static Depot createMainDepot() {
        return new Depot(
                "MAIN-DEP",
                Constants.MAIN_DEPOT_LOCATION,
                10000,
                DepotType.MAIN);
    }

    private static List<Depot> createAuxiliaryDepots() {
        List<Depot> auxDepots = new ArrayList<>();

        // Depósito auxiliar norte
        Depot northDepot = new Depot(
                "AUX-NORTH",
                Constants.NORTH_DEPOT_LOCATION,
                5000,
                DepotType.AUXILIARY);
        northDepot.refill(); // Llenar el depósito
        auxDepots.add(northDepot);

        // Depósito auxiliar este
        Depot eastDepot = new Depot(
                "AUX-EAST",
                Constants.EAST_DEPOT_LOCATION,
                5000,
                DepotType.AUXILIARY);
        eastDepot.refill(); // Llenar el depósito
        auxDepots.add(eastDepot);

        return auxDepots;
    }

    private static List<Order> createTestOrders(LocalDateTime currentTime) {
        List<Order> orders = new ArrayList<>();

        // Orden 1 - Centro de la ciudad
        Order order1 = Order.builder()
                .id("ORD-001")
                .arrivalTime(currentTime.minusHours(2))
                .deadlineTime(currentTime.plusHours(6))
                .glpRequestM3(10)
                .position(new Position(20, 15))
                .build();
        orders.add(order1);

        // Orden 2 - Sur de la ciudad
        Order order2 = Order.builder()
                .id("ORD-002")
                .arrivalTime(currentTime.minusHours(1))
                .deadlineTime(currentTime.plusHours(8))
                .glpRequestM3(5)
                .position(new Position(30, 5))
                .build();
        orders.add(order2);

        // Orden 3 - Norte de la ciudad (cerca del depósito auxiliar norte)
        Order order3 = Order.builder()
                .id("ORD-003")
                .arrivalTime(currentTime.minusMinutes(30))
                .deadlineTime(currentTime.plusHours(4))
                .glpRequestM3(15)
                .position(new Position(45, 40))
                .build();
        orders.add(order3);

        // Orden 4 - Este de la ciudad (cerca del depósito auxiliar este)
        Order order4 = Order.builder()
                .id("ORD-004")
                .arrivalTime(currentTime)
                .deadlineTime(currentTime.plusHours(10))
                .glpRequestM3(8)
                .position(new Position(60, 10))
                .build();
        orders.add(order4);

        return orders;
    }
}
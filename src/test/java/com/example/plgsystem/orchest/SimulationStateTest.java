package com.example.plgsystem.orchest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.operation.VehiclePlanCreator;
import com.example.plgsystem.simulation.SimulationState;

public class SimulationStateTest {

    private SimulationState state;
    private LocalDateTime referenceTime;
    private Depot mainDepot;
    private List<Vehicle> vehicles;
    private List<Depot> auxDepots;

    @BeforeEach
    void setUp() {
        referenceTime = LocalDateTime.of(2023, 1, 1, 8, 0);
        mainDepot = new Depot("MD001", new Position(0, 0), 10000, DepotType.MAIN);
        mainDepot.refill(); // Ensure depot has GLP

        vehicles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vehicles.add(new Vehicle("V-" + i, VehicleType.TA, mainDepot.getPosition()));
        }

        auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AD001", new Position(50, 50), 500, DepotType.AUXILIARY));
        auxDepots.add(new Depot("AD002", new Position(100, 100), 500, DepotType.AUXILIARY));

        // Refill auxiliary depots
        for (Depot depot : auxDepots) {
            depot.refill();
        }

        state = new SimulationState(vehicles, mainDepot, auxDepots, referenceTime);
    }

    @Test
    void testBasicStateCreation() {
        assertEquals(referenceTime, state.getCurrentTime(), "Current time should match reference time");
        assertEquals(5, state.getVehicles().size(), "Should have 5 vehicles");
        assertEquals(mainDepot, state.getMainDepot(), "Main depot should be set correctly");
        assertEquals(2, state.getAuxDepots().size(), "Should have 2 auxiliary depots");
    }

    @Test
    void testSnapshotCreation() {
        // Add some orders to the state
        List<Order> orders = createSampleOrders(5);
        for (Order order : orders) {
            state.addOrder(order);
        }

        // Create snapshot
        SimulationState snapshot = state.createSnapshot();

        // Verify snapshot properties
        assertEquals(state.getCurrentTime(), snapshot.getCurrentTime(), "Snapshot time should match original");
        assertEquals(state.getVehicles().size(), snapshot.getVehicles().size(), "Vehicle count should match");
        assertEquals(state.getOrders().size(), snapshot.getOrders().size(), "Order count should match");

        // Verify independence - changing original shouldn't affect snapshot
        state.addOrder(new Order("NEW-ORDER", referenceTime, referenceTime.plusHours(2), 100, new Position(25, 25)));
        assertEquals(6, state.getOrders().size(), "Original should have new order added");
        assertEquals(5, snapshot.getOrders().size(), "Snapshot should not be affected by changes to original");
    }

    @Test
    void testAdvanceTimeWithoutPlans() {
        // Add some orders
        List<Order> orders = createSampleOrders(3);
        for (Order order : orders) {
            state.addOrder(order);
        }

        // Capture initial state
        LocalDateTime initialTime = state.getCurrentTime();

        // Advance time
        state.advanceTime(Duration.ofHours(2));

        // Verify time advanced correctly
        assertEquals(initialTime.plusHours(2), state.getCurrentTime(), "Time should advance by 2 hours");

        // Verify no changes to vehicles or orders (since no plans)
        for (Vehicle vehicle : state.getVehicles()) {
            assertEquals(mainDepot.getPosition(), vehicle.getCurrentPosition(),
                    "Vehicle should not move without plans");
            assertEquals(vehicle.getFuelCapacityGal(), vehicle.getCurrentFuelGal(),
                    "Fuel should not change without plans");
        }

        for (Order order : state.getOrders()) {
            assertEquals(order.getGlpRequestM3(), order.getRemainingGlpM3(),
                    "Orders should not be served without plans");
        }
    }

    @Test
    void testAdvanceTimeWithMetaheuristicSolver() {
        List<Order> orders = createSampleOrders(10);
        for (Order order : orders) {
            state.addOrder(order);
        }

        System.out.println("Initial state:");
        System.out.println("Main depot GLP: " + mainDepot.getCurrentGlpM3());

        // Create plans using MetaheuristicSolver
        Solution solution = MetaheuristicSolver.solve(state);

        System.out.println("Solution routes:");
        for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, state);
            assertNotNull(plan, "Plan should be created");
            state.addVehiclePlan(vehicleId, plan);
        }

        Map<String, Integer> originalOrderStatus = new HashMap<>();
        for (Order order : orders) {
            originalOrderStatus.put(order.getId(), order.getRemainingGlpM3());
        }

        // Advance time by 24 hours
        state.advanceTime(Duration.ofHours(24));
        Map<String, Integer> newOrderStatus = new HashMap<>();
        for (Order order : state.getOrders()) {
            newOrderStatus.put(order.getId(), order.getRemainingGlpM3());
        }

        System.out.println("Original order status: " + originalOrderStatus);
        System.out.println("New order status: " + newOrderStatus);

        assertEquals(referenceTime.plusHours(24), state.getCurrentTime(), "Time should advance by 24 hours");
        assertTrue(newOrderStatus.size() <= originalOrderStatus.size(), "There should be less or equal orders in the state");
        // Check for any Changes in the orders
        boolean anyOrderChanged = false;
        for (Order order : orders) {
            // check if the order is served by not found in the newOrderStatus
            if (!newOrderStatus.containsKey(order.getId())) {
                anyOrderChanged = true;
                break;
            }
            // else check if the order is served by checking the remaining GLP
            if (newOrderStatus.get(order.getId()) < originalOrderStatus.get(order.getId())) {
                anyOrderChanged = true;
                break;
            }
        }
        assertTrue(anyOrderChanged, "At least one order should be served");
    }

    @Test
    void testAdvanceTimeWithZeroDuration() {
        // Add an order
        Order order = new Order("ORDER-1", referenceTime, referenceTime.plusHours(5), 50, new Position(20, 20));
        state.addOrder(order);

        // Create plan for a vehicle
        Solution solution = MetaheuristicSolver.solve(state);
        for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, state);
            state.addVehiclePlan(vehicleId, plan);
        }

        // Save initial time for comparison
        LocalDateTime initialTime = state.getCurrentTime();

        // Advance time by zero duration
        state.advanceTime(Duration.ZERO);

        // Verify nothing changed
        assertEquals(initialTime, state.getCurrentTime(), "Time should not change with zero duration");
        assertEquals(50, order.getRemainingGlpM3(), "Order should not be served with zero duration");
    }

    // Helper methods

    private List<Order> createSampleOrders(int count) {
        Random random = new Random(42); // Use fixed seed for reproducibility
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int glpRequest = 20 + random.nextInt(80); // 20-100 m3
            int x = random.nextInt(100);
            int y = random.nextInt(100);
            int durationHours = 4 + random.nextInt(20); // 4-24 hours

            orders.add(new Order(
                    "ORD-" + i,
                    referenceTime,
                    referenceTime.plusHours(durationHours),
                    glpRequest,
                    new Position(x, y)));
        }
        return orders;
    }
}
package com.example.plgsystem.orchest;

import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.*;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.operation.VehiclePlanCreator;
import com.example.plgsystem.simulation.SimulationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationStateSnapshotTest {

    private SimulationState state;

    @BeforeEach
    void setUp() {
        state = createSampleState();
    }

    @Test
    void testSnapshotCreation() {
        // Arrange
        List<Order> orders = createSampleOrders(10);
        for (Order order : orders) {
            state.addOrder(order);
        }

        // Act
        SimulationState snapshot = state.createSnapshot();
        assertEquals(state.getCurrentTime(), snapshot.getCurrentTime());
        assertEquals(state.getVehicles(), snapshot.getVehicles());
        assertEquals(state.getMainDepot(), snapshot.getMainDepot());
        assertEquals(state.getAuxDepots(), snapshot.getAuxDepots());
        assertEquals(state.getOrders(), snapshot.getOrders());
        assertEquals(state.getBlockages(), snapshot.getBlockages());
        assertEquals(state.getOrders(), snapshot.getOrders());
    }

    @Test
    void testSnapshotAndProjection() {
        // Arrange
        List<Order> orders = createSampleOrders(10);
        for (Order order : orders) {
            state.addOrder(order);
        }

        // Act
        Solution solution = MetaheuristicSolver.solve(state);
        for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, state);
            state.addVehiclePlan(vehicleId, plan);
        }

        // Take snapshot of original state values
        Map<String, Integer> initialOrderRemainingGlp = new HashMap<>();
        Map<String, Position> initialVehiclePositions = new HashMap<>();

        for (Order order : state.getOrders()) {
            initialOrderRemainingGlp.put(order.getId(), order.getRemainingGlpM3());
        }

        for (Vehicle vehicle : state.getVehicles()) {
            initialVehiclePositions.put(vehicle.getId(),
                    vehicle.getCurrentPosition() != null ? vehicle.getCurrentPosition().clone()
                            : null);
        }

        // Create snapshot and advance time
        SimulationState snapshot = state.createSnapshot();
        snapshot.advanceTime(Duration.ofHours(4));

        // Assert that state has changed in the snapshot
        boolean anyOrderChanged = false;
        boolean anyVehicleMoved = false;

        // Check if any orders have been served
        for (Order order : snapshot.getOrders()) {
            int initialGlp = initialOrderRemainingGlp.getOrDefault(order.getId(), -1);
            if (initialGlp != order.getRemainingGlpM3()) {
                anyOrderChanged = true;
                break;
            }
        }

        // Check if any vehicles have moved
        for (Vehicle vehicle : snapshot.getVehicles()) {
            Position initialPos = initialVehiclePositions.get(vehicle.getId());
            Position currentPos = vehicle.getCurrentPosition();

            if (initialPos != null && currentPos != null && !initialPos.equals(currentPos)) {
                anyVehicleMoved = true;
                break;
            }
        }

        // Assert that the simulation has progressed with vehicles moving and orders
        // being served
        assertTrue(anyOrderChanged || anyVehicleMoved,
                "After advancing time, either orders should be served or vehicles should move");

        // Verify original state remains unchanged
        for (Order order : state.getOrders()) {
            assertEquals(initialOrderRemainingGlp.get(order.getId()), order.getRemainingGlpM3(),
                    "Original state orders should remain unchanged");
        }

        for (Vehicle vehicle : state.getVehicles()) {
            Position initialPos = initialVehiclePositions.get(vehicle.getId());
            assertTrue(initialPos.equals(vehicle.getCurrentPosition()),
                    "Original state vehicle positions should remain unchanged");
        }
    }

    private SimulationState createSampleState() {
        Depot mainDepot = new Depot("MD001", new Position(0, 0), 10000, DepotType.MAIN);
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vehicles.add(new Vehicle("V-" + i, VehicleType.TA, mainDepot.getPosition()));
        }
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AD001", new Position(50, 50), 160, DepotType.AUXILIARY));
        auxDepots.add(new Depot("AD002", new Position(100, 100), 160, DepotType.AUXILIARY));
        return new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
    }

    private List<Order> createSampleOrders(int numberOfOrders) {
        Random random = new Random();
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            int glpRequestM3 = random.nextInt(100);
            int pos_x = random.nextInt(70);
            int pos_y = random.nextInt(70);
            int durationHours = random.nextInt(24 * 3 - 4) + 4;
            Position position = new Position(pos_x, pos_y);
            orders.add(new Order("ORD-" + i, LocalDateTime.now(),
                    LocalDateTime.now().plusHours(durationHours), glpRequestM3, position));
        }
        return orders;
    }
}
package com.example.plgsystem.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionFactory;
import com.example.plgsystem.operation.VehiclePlan;

public class PlanExecutorTest {

    private SimulationState state;
    private LocalDateTime startTime;
    private Vehicle vehicle;
    private Depot mainDepot;
    private Depot auxDepot;
    private Order order;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2023, 1, 1, 8, 0);

        // Create test environment
        mainDepot = new Depot("MD001", new Position(0, 0), 10000, DepotType.MAIN);
        mainDepot.refill();

        auxDepot = new Depot("AD001", new Position(50, 50), 500, DepotType.AUXILIARY);
        auxDepot.refill();

        vehicle = new Vehicle("V-001", VehicleType.TA, mainDepot.getPosition());

        order = new Order("ORD-001", startTime, startTime.plusHours(5), 50, new Position(20, 20));

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle);

        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(auxDepot);

        state = new SimulationState(vehicles, mainDepot, auxDepots, startTime);
        state.addOrder(order);
    }

    @Test
    void testDriveAction() {
        // Create a DRIVE action
        Position start = new Position(0, 0);
        Position end = new Position(20, 20);
        List<Position> path = Arrays.asList(start, new Position(10, 10), end);

        double distance = start.distanceTo(end);
        double fuelNeeded = vehicle.calculateFuelNeeded(distance);
        double initialFuel = vehicle.getCurrentFuelGal();

        LocalDateTime actionStart = startTime;
        LocalDateTime actionEnd = actionStart.plusHours(1); // 1 hour to drive

        Action driveAction = ActionFactory.createDrivingAction(
                path,
                fuelNeeded,
                actionStart,
                actionEnd
        );

        // Create and add a vehicle plan with the drive action
        List<Action> actions = new ArrayList<>();
        actions.add(driveAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Execute plan with half-completed action
        LocalDateTime halfwayTime = actionStart.plusMinutes(30);
        PlanExecutor.executePlan(state, halfwayTime);

        // Check vehicle state at halfway point
        assertEquals(VehicleStatus.DRIVING, vehicle.getStatus(), "Vehicle should be in DRIVING status");
        assertTrue(vehicle.getCurrentFuelGal() < initialFuel, "Fuel should be consumed");
        assertTrue(vehicle.getCurrentPosition().getX() > 0 && vehicle.getCurrentPosition().getX() < 20,
                "Vehicle should be somewhere between start and end X");
        assertTrue(vehicle.getCurrentPosition().getY() > 0 && vehicle.getCurrentPosition().getY() < 20,
                "Vehicle should be somewhere between start and end Y");
        assertEquals(0.5, driveAction.getCurrentProgress(), 0.1, "Progress should be about 50%");

        // Complete the action
        PlanExecutor.executePlan(state, actionEnd);

        // Check vehicle state at completion
        assertEquals(1.0, driveAction.getCurrentProgress(), "Action should be complete");
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(),
                "Vehicle should be AVAILABLE after completing action");
        assertEquals(end.getX(), vehicle.getCurrentPosition().getX(), 0.001, "Vehicle should be at destination X");
        assertEquals(end.getY(), vehicle.getCurrentPosition().getY(), 0.001, "Vehicle should be at destination Y");
        System.out.println(String.format("initialFuel: %.2f || fuelNeeded: %.2f || currentFuel: %.2f", initialFuel, fuelNeeded, vehicle.getCurrentFuelGal()));
        assertEquals(initialFuel - fuelNeeded, vehicle.getCurrentFuelGal(), 0.001, "Fuel should be consumed and equal to fuelneeded");
    }

    @Test
    void testReloadAction() {
        // Ensure vehicle has no GLP
        vehicle.dispense(vehicle.getCurrentGlpM3());
        int initialGlp = vehicle.getCurrentGlpM3();
        assertEquals(0, initialGlp, "Vehicle should start with no GLP");

        // Amount to load
        int glpToLoad = 10;

        // Create a RELOAD action
        LocalDateTime actionStart = startTime;
        LocalDateTime actionEnd = actionStart.plusMinutes(45); // 45 minutes to reload

        Action reloadAction = ActionFactory.createRefillingAction(
                auxDepot.getId(),
                auxDepot.getPosition(),
                actionStart,
                glpToLoad
        );

        // Create and add a vehicle plan with the reload action
        List<Action> actions = new ArrayList<>();
        actions.add(reloadAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Record initial depot GLP
        int initialDepotGlp = auxDepot.getCurrentGlpM3();

        // Execute plan
        PlanExecutor.executePlan(state, actionEnd);

        // Check vehicle and depot state
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(),
                "Vehicle should be AVAILABLE after completing reload");
        assertEquals(glpToLoad, vehicle.getCurrentGlpM3(), "Vehicle should have loaded the correct amount of GLP");
        assertEquals(initialDepotGlp - glpToLoad, auxDepot.getCurrentGlpM3(), "Depot should have decreased GLP");
        assertEquals(1.0, reloadAction.getCurrentProgress(), "Action should be complete");
        assertTrue(reloadAction.isEffectApplied(), "Effect should be marked as applied");
    }

    @Test
    void testServeAction() {
        // Prepare vehicle with GLP
        int glpToDeliver = 5;
        vehicle.refill(glpToDeliver);
        int initialVehicleGlp = vehicle.getCurrentGlpM3();
        int initialOrderRemaining = order.getRemainingGlpM3();

        // Create a SERVE action
        LocalDateTime actionStart = startTime;
        LocalDateTime actionEnd = actionStart.plusMinutes(20); // 20 minutes to serve

        Action serveAction = ActionFactory.createServingAction(
                order.getPosition(),
                order.getId(),
                glpToDeliver,
                actionStart
        );

        // Create and add a vehicle plan with the serve action
        List<Action> actions = new ArrayList<>();
        actions.add(serveAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Execute plan
        PlanExecutor.executePlan(state, actionEnd);

        // Check vehicle and order state
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(),
                "Vehicle should be AVAILABLE after completing serve");
        assertEquals(initialVehicleGlp - glpToDeliver, vehicle.getCurrentGlpM3(), "Vehicle should have decreased GLP");
        assertEquals(initialOrderRemaining - glpToDeliver, order.getRemainingGlpM3(),
                "Order should have decreased remaining GLP");
        assertEquals(1.0, serveAction.getCurrentProgress(), "Action should be complete");
        assertTrue(serveAction.isEffectApplied(), "Effect should be marked as applied");
        assertEquals(1, order.getServeRecords().size(), "Order should have a serve record");
    }

    @Test
    void testWaitAction() {
        // Create a WAIT action
        LocalDateTime actionStart = startTime;
        LocalDateTime actionEnd = actionStart.plusHours(1); // Wait for 1 hour
        Duration waitDuration = Duration.between(actionStart, actionEnd);

        Action waitAction = ActionFactory.createIdleAction(
                vehicle.getCurrentPosition(),
                waitDuration,
                actionStart
        );

        // Create and add a vehicle plan with the wait action
        List<Action> actions = new ArrayList<>();
        actions.add(waitAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Execute plan halfway
        LocalDateTime halfwayTime = actionStart.plusMinutes(30);
        PlanExecutor.executePlan(state, halfwayTime);

        // Check vehicle status
        assertEquals(VehicleStatus.IDLE, vehicle.getStatus(), "Vehicle should be IDLE during wait");
        assertEquals(0.5, waitAction.getCurrentProgress(), 0.1, "Progress should be about 50%");

        // Complete the action
        PlanExecutor.executePlan(state, actionEnd);

        // Check completion
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(), "Vehicle should be AVAILABLE after wait");
        assertEquals(1.0, waitAction.getCurrentProgress(), "Action should be complete");
    }

    @Test
    void testMaintenanceAction() {
        // Create a MAINTENANCE action
        LocalDateTime actionStart = startTime;
        LocalDateTime actionEnd = actionStart.plusHours(2); // 2 hours maintenance
        Duration maintenanceDuration = Duration.between(actionStart, actionEnd);

        Action maintenanceAction = ActionFactory.createMaintenanceAction(
                vehicle.getCurrentPosition(),
                maintenanceDuration,
                actionStart
        );

        // Create and add a vehicle plan with the maintenance action
        List<Action> actions = new ArrayList<>();
        actions.add(maintenanceAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Execute plan halfway
        LocalDateTime halfwayTime = actionStart.plusHours(1);
        PlanExecutor.executePlan(state, halfwayTime);

        // Check vehicle status
        assertEquals(VehicleStatus.MAINTENANCE, vehicle.getStatus(), "Vehicle should be in MAINTENANCE status");
        assertEquals(0.5, maintenanceAction.getCurrentProgress(), 0.1, "Progress should be about 50%");

        // Complete the action
        PlanExecutor.executePlan(state, actionEnd);

        // Check completion
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(), "Vehicle should be AVAILABLE after maintenance");
        assertEquals(1.0, maintenanceAction.getCurrentProgress(), "Action should be complete");
    }

    @Test
    void testSequentialActions() {
        // Create a sequence of actions: DRIVE -> SERVE -> DRIVE back
        Position startPos = vehicle.getCurrentPosition().clone();
        Position orderPos = order.getPosition().clone();

        // Calculate distances and fuel needed
        double distanceToOrder = startPos.distanceTo(orderPos);
        double fuelToOrder = vehicle.calculateFuelNeeded(distanceToOrder);
        double fuelBackToDepot = vehicle.calculateFuelNeeded(distanceToOrder); // Same distance back

        // Prepare vehicle with GLP
        int glpToDeliver = 5;
        vehicle.refill(glpToDeliver);

        // Create actions
        LocalDateTime actionStart = startTime;
        LocalDateTime driveEnd = actionStart.plusHours(1);
        LocalDateTime serveEnd = driveEnd.plusMinutes(20);
        LocalDateTime returnEnd = serveEnd.plusHours(1);

        // Drive to order
        Action driveToAction = ActionFactory.createDrivingAction(
                Arrays.asList(startPos, orderPos),
                fuelToOrder,
                actionStart,
                driveEnd);

        // Serve order
        Action serveAction = ActionFactory.createServingAction(
                orderPos,
                order.getId(),
                glpToDeliver,
                driveEnd);

        // Drive back to depot
        Action returnAction = ActionFactory.createDrivingAction(
                Arrays.asList(orderPos, startPos),
                fuelBackToDepot,
                serveEnd,
                returnEnd);

        // Create and add a vehicle plan with all actions
        List<Action> actions = Arrays.asList(driveToAction, serveAction, returnAction);
        VehiclePlan plan = new VehiclePlan(vehicle.getId(), actions, actionStart, 0);
        state.addVehiclePlan(vehicle.getId(), plan);

        // Initial state checks
        int initialGlp = vehicle.getCurrentGlpM3();
        int initialOrderRemaining = order.getRemainingGlpM3();

        // Execute first action (drive to order)
        PlanExecutor.executePlan(state, driveEnd);
        assertEquals(VehicleStatus.SERVING, vehicle.getStatus(), "Vehicle should be SERVING after drive");
        assertEquals(orderPos.getX(), vehicle.getCurrentPosition().getX(), 0.001,
                "Vehicle should be at order location X");
        assertEquals(orderPos.getY(), vehicle.getCurrentPosition().getY(), 0.001,
                "Vehicle should be at order location Y");
        
        // Serving order
        assertEquals(initialGlp - glpToDeliver, vehicle.getCurrentGlpM3(), "GLP should be delivered");
        assertEquals(initialOrderRemaining - glpToDeliver, order.getRemainingGlpM3(), "Order should be partially served");
        assertEquals(1.0, driveToAction.getCurrentProgress(), "First action should be complete");
        assertEquals(0.0, serveAction.getCurrentProgress(), "Second action should not have started");

        // Execute second action (serve order)
        PlanExecutor.executePlan(state, serveEnd);
        assertEquals(VehicleStatus.DRIVING, vehicle.getStatus(), "Vehicle should be DRIVING after serve");
        assertEquals(initialGlp - glpToDeliver, vehicle.getCurrentGlpM3(), "GLP should be delivered");
        assertEquals(initialOrderRemaining - glpToDeliver, order.getRemainingGlpM3(),
                "Order should be fully served");
        assertEquals(1.0, serveAction.getCurrentProgress(), "Second action should be complete");
        assertEquals(0.0, returnAction.getCurrentProgress(), "Third action should not have started");

        // Execute third action (return to depot)
        PlanExecutor.executePlan(state, returnEnd);
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus(), "Vehicle should be AVAILABLE after return");
        assertEquals(startPos.getX(), vehicle.getCurrentPosition().getX(), 0.001, "Vehicle should be back at start X");
        assertEquals(startPos.getY(), vehicle.getCurrentPosition().getY(), 0.001, "Vehicle should be back at start Y");
        assertEquals(1.0, returnAction.getCurrentProgress(), "Third action should be complete");
    }
}
package com.example.plgsystem.assignation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

class SolutionTest {

    private SimulationState mockState;

    @BeforeEach
    void setUp() {
        Order order1 = new Order("ORD-1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), 10,
                new Position(0, 0));
        Order order2 = new Order("ORD-2", LocalDateTime.now(), LocalDateTime.now().plusHours(2), 15,
                new Position(0, 0));
        Vehicle vehicle1 = new Vehicle("V-001", VehicleType.TA, new Position(0, 0));
        Depot depot1 = new Depot("DEP-1", new Position(0, 0), 100, DepotType.MAIN);
        Depot depot2 = new Depot("DEP-2", new Position(0, 0), 50, DepotType.AUXILIARY);

        mockState = new SimulationState(List.of(vehicle1), depot1, List.of(depot2), LocalDateTime.now());
        mockState.addOrder(order1);
        mockState.addOrder(order2);
    }

    @Test
    void testSolutionCreation() {
        // Arrange
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
                new RouteStop(new Position(0, 0), "ORD-1", LocalDateTime.now().plusHours(1), 10),
                new RouteStop(new Position(0, 0), "DEP-1", 20),
                new RouteStop(new Position(0, 0), "ORD-2", LocalDateTime.now().plusHours(2), 15));
        routes.put("V-001", new Route("V-001", stops, LocalDateTime.now()));

        // Act
        Solution solution = new Solution(routes, mockState);

        // Assert
        assertEquals(routes, solution.getRoutes());
        assertTrue(solution.getCost().totalCost() > 0);
    }

    @Test
    void testGetVehicleOrderAssignments() {
        // Arrange
        Map<String, Route> routes = new HashMap<>();

        // Create route with mixed stops
        List<RouteStop> stops1 = Arrays.asList(
                new RouteStop(new Position(0, 0), "ORD-1", LocalDateTime.now().plusHours(1), 5),
                new RouteStop(new Position(0, 0), "DEP-1", 20),
                new RouteStop(new Position(0, 0), "ORD-2", LocalDateTime.now().plusHours(2), 10));
        routes.put("V-001", new Route("V-001", stops1, LocalDateTime.now()));

        // Create another route with only order stops
        List<RouteStop> stops2 = Arrays.asList(
                new RouteStop(new Position(0, 0), "ORD-3", LocalDateTime.now().plusHours(1), 7),
                new RouteStop(new Position(0, 0), "ORD-4", LocalDateTime.now().plusHours(3), 3));
        routes.put("V-002", new Route("V-002", stops2, LocalDateTime.now()));

        Solution solution = new Solution(routes, mockState);

        // Act
        Map<String, List<DeliveryPart>> assignments = solution.getVehicleOrderAssignments();

        // Assert
        assertEquals(2, assignments.size());
        assertEquals(2, assignments.get("V-001").size());
        assertEquals(2, assignments.get("V-002").size());

        // Verify correct conversion from RouteStop to DeliveryPart
        assertEquals("ORD-1", assignments.get("V-001").get(0).getOrderId());
        assertEquals(5, assignments.get("V-001").get(0).getGlpDeliverM3());

        assertEquals("ORD-2", assignments.get("V-001").get(1).getOrderId());
        assertEquals(10, assignments.get("V-001").get(1).getGlpDeliverM3());

        assertEquals("ORD-3", assignments.get("V-002").get(0).getOrderId());
        assertEquals(7, assignments.get("V-002").get(0).getGlpDeliverM3());

        assertEquals("ORD-4", assignments.get("V-002").get(1).getOrderId());
        assertEquals(3, assignments.get("V-002").get(1).getGlpDeliverM3());
    }
}
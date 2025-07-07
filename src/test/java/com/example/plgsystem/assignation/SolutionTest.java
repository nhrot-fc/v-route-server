package com.example.plgsystem.assignation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SolutionTest {

    @Test
    void testSolutionCreation() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 10);
        ordersState.put("ORD-2", 15);
        
        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);
        depotsState.put("DEP-2", 50);
        
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
            new RouteStop("ORD-1", LocalDateTime.now().plusHours(1), 10),
            new RouteStop("DEP-1", 20),
            new RouteStop("ORD-2", LocalDateTime.now().plusHours(2), 15)
        );
        routes.put("V-001", new Route("V-001", stops, LocalDateTime.now()));
        
        // Act
        Solution solution = new Solution(ordersState, depotsState, routes);
        
        // Assert
        assertEquals(ordersState, solution.getOrdersState());
        assertEquals(depotsState, solution.getDepotsState());
        assertEquals(routes, solution.getRoutes());
        assertEquals(0.0, solution.getCost());
    }
    
    @Test
    void testSolutionWithCost() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        Map<String, Integer> depotsState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();
        double cost = 150.5;
        
        // Act
        Solution solution = new Solution(ordersState, depotsState, routes, cost);
        
        // Assert
        assertEquals(cost, solution.getCost());
    }
    
    @Test
    void testGetVehicleOrderAssignments() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        Map<String, Integer> depotsState = new HashMap<>();
        Map<String, Route> routes = new HashMap<>();
        
        // Create route with mixed stops
        List<RouteStop> stops1 = Arrays.asList(
            new RouteStop("ORD-1", LocalDateTime.now().plusHours(1), 5),
            new RouteStop("DEP-1", 20),
            new RouteStop("ORD-2", LocalDateTime.now().plusHours(2), 10)
        );
        routes.put("V-001", new Route("V-001", stops1, LocalDateTime.now()));
        
        // Create another route with only order stops
        List<RouteStop> stops2 = Arrays.asList(
            new RouteStop("ORD-3", LocalDateTime.now().plusHours(1), 7),
            new RouteStop("ORD-4", LocalDateTime.now().plusHours(3), 3)
        );
        routes.put("V-002", new Route("V-002", stops2, LocalDateTime.now()));
        
        Solution solution = new Solution(ordersState, depotsState, routes);
        
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
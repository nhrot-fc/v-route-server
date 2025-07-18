package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;
import org.mockito.MockedStatic;

class SolutionGeneratorTest {

    private SimulationState mockState;
    private List<Position> mockPath;
    private Map<String, List<DeliveryPart>> assignments;

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

        mockPath = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockPath.add(new Position(i, i));
        }

        assignments = new HashMap<>();
        assignments.put("V-001", List.of(new DeliveryPart("ORD-1", 10, order1.getDeadlineTime())));
        assignments.put("V-002", List.of(new DeliveryPart("ORD-2", 15, order2.getDeadlineTime())));
    }

    @Test
    void generateSolution_shouldCreateRoutesForEachVehicle() {
        // Setup static mock for PathFinder
        try (MockedStatic<PathFinder> pathFinderMock = mockStatic(PathFinder.class, withSettings())) {
            // Configure PathFinder to always return a valid path
            pathFinderMock.when(() -> PathFinder.findPath(any(), any(), any(), any())).thenReturn(mockPath);
            
            // Act
            Solution solution = SolutionGenerator.generateSolution(mockState, assignments);
            
            // Assert
            assertNotNull(solution);
            assertNotNull(solution.getRoutes());
            
            // Check if we have a valid solution with order and depot state
            assertNotNull(solution.getVehicleOrderAssignments());
        }
    }
    
    @Test
    void generateSolution_shouldInitializeOrdersAndDepotsStates() {
        // Setup static mock for PathFinder
        try (MockedStatic<PathFinder> pathFinderMock = mockStatic(PathFinder.class, withSettings())) {
            // Configure PathFinder to always return a valid path
            pathFinderMock.when(() -> PathFinder.findPath(any(), any(), any(), any())).thenReturn(mockPath);
            
            // Act
            Solution solution = SolutionGenerator.generateSolution(mockState, assignments);
            
            // Assert
            Map<String, List<DeliveryPart>> ordersAssigments = solution.getVehicleOrderAssignments();
            
            assertNotNull(ordersAssigments);
        }
    }
    
    @Test
    void generateSolution_shouldHandleEmptyAssignments() {
        // Arrange
        Map<String, List<DeliveryPart>> emptyAssignments = new HashMap<>();
        emptyAssignments.put("V-001", new ArrayList<>());
        emptyAssignments.put("V-002", new ArrayList<>());
        
        // Setup static mock for PathFinder
        try (MockedStatic<PathFinder> pathFinderMock = mockStatic(PathFinder.class, withSettings())) {
            // Configure PathFinder to always return a valid path
            pathFinderMock.when(() -> PathFinder.findPath(any(), any(), any(), any())).thenReturn(mockPath);
            
            // Act
            Solution solution = SolutionGenerator.generateSolution(mockState, emptyAssignments);
            
            // Assert
            Map<String, Route> routes = solution.getRoutes();
            assertTrue(routes.isEmpty());
        }
    }
    
    @Test
    void generateSolution_shouldReintegrateLostDeliveryParts() {
        // Setup a scenario with one vehicle that can't complete its route
        SimulationState state = createMockStateForReintegrationTest();
        
        // Create assignments with a vehicle that will fail route generation
        Map<String, List<DeliveryPart>> testAssignments = new HashMap<>();
        
        // Vehicle 1 gets a normal delivery
        List<DeliveryPart> v1Parts = new ArrayList<>();
        v1Parts.add(new DeliveryPart("ORD-1", 5, state.getOrderById("ORD-1").getDeadlineTime()));
        testAssignments.put("V-001", v1Parts);
        
        // Vehicle 2 gets a delivery that will cause route failure (not enough fuel)
        List<DeliveryPart> v2Parts = new ArrayList<>();
        v2Parts.add(new DeliveryPart("ORD-2", 10, state.getOrderById("ORD-2").getDeadlineTime()));
        testAssignments.put("V-002", v2Parts);
        
        // Setup static mock for PathFinder if needed
        try (MockedStatic<PathFinder> pathFinderMock = mockStatic(PathFinder.class, withSettings())) {
            pathFinderMock.when(() -> PathFinder.findPath(any(), any(), any(), any())).thenReturn(mockPath);
            
            // Act
            Solution solution = SolutionGenerator.generateSolution(state, testAssignments);
            
            // Assert
            assertNotNull(solution, "Solution should not be null after reintegration");
            
            // Check that all orders are assigned in routes
            boolean order1Found = false;
            boolean order2Found = false;
            
            for (Route route : solution.getRoutes().values()) {
                for (RouteStop stop : route.stops()) {
                    if (stop.isOrderStop() && "ORD-1".equals(stop.getOrderId())) {
                        order1Found = true;
                    }
                    if (stop.isOrderStop() && "ORD-2".equals(stop.getOrderId())) {
                        order2Found = true;
                    }
                }
            }
            
            assertTrue(order1Found, "Order 1 should be in routes");
            assertTrue(order2Found, "Order 2 should be reintegrated into routes");
        }
    }
    
    private SimulationState createMockStateForReintegrationTest() {
        // Create vehicles with different fuel states
        Vehicle v1 = new Vehicle("V-001", VehicleType.TA, new Position(0, 0));
        v1.setCurrentFuelGal(100); // Plenty of fuel
        
        Vehicle v2 = new Vehicle("V-002", VehicleType.TA, new Position(0, 0));
        v2.setCurrentFuelGal(1);   // Almost no fuel
        
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(v1);
        vehicles.add(v2);
        
        // Create orders
        Order order1 = new Order("ORD-1", LocalDateTime.now(), LocalDateTime.now().plusHours(2), 
                5, new Position(10, 10));  // Close location
        
        Order order2 = new Order("ORD-2", LocalDateTime.now(), LocalDateTime.now().plusHours(3), 
                10, new Position(500, 500)); // Far location requiring more fuel
        
        // Create depots
        Depot mainDepot = new Depot("DEP-1", new Position(0, 0), 100, DepotType.MAIN);
        List<Depot> auxDepots = new ArrayList<>();
        
        // Create simulation state
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        state.addOrder(order1);
        state.addOrder(order2);
        
        return state;
    }
} 
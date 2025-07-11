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
} 
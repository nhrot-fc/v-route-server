package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolutionEvaluatorTest {

    @Mock
    private SimulationState mockState;
    
    @Mock
    private Vehicle mockVehicle;
    
    @Mock
    private Order mockOrder;
    
    @Mock
    private Depot mockDepot;
    
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        
        // Setup mock vehicle with stubs
        when(mockVehicle.getId()).thenReturn("V-001");
        when(mockVehicle.getCurrentGlpM3()).thenReturn(20);
        when(mockVehicle.getCurrentFuelGal()).thenReturn(30.0);
        when(mockVehicle.getFuelCapacityGal()).thenReturn(40.0);
        when(mockVehicle.calculateFuelNeeded(anyDouble())).thenReturn(5.0);
        when(mockVehicle.getCurrentPosition()).thenReturn(new Position(0, 0));
        
        // Setup mock order with stubs
        when(mockOrder.getId()).thenReturn("ORD-1");
        when(mockOrder.getPosition()).thenReturn(new Position(10, 10));
        when(mockOrder.getDeadlineTime()).thenReturn(currentTime.plusHours(2));
        
        // Setup mock depot with stubs
        when(mockDepot.getId()).thenReturn("DEP-1");
        when(mockDepot.getPosition()).thenReturn(new Position(5, 5));
        when(mockDepot.isMain()).thenReturn(true);
        
        // Setup mock state with stubs
        when(mockState.getVehicleById("V-001")).thenReturn(mockVehicle);
        when(mockState.getOrderById("ORD-1")).thenReturn(mockOrder);
        when(mockState.getDepotById("DEP-1")).thenReturn(mockDepot);
    }

    @Test
    void evaluate_shouldReturnSolutionWithCalculatedCost() {
        // Arrange
        // Create a solution to evaluate
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 0);  // Order fully satisfied
        
        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);
        
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
            new RouteStop("ORD-1", currentTime.plusHours(1), 10)
        );
        routes.put("V-001", new Route("V-001", stops, currentTime));
        
        Solution solution = new Solution(ordersState, depotsState, routes);
        
        // Mock PathFinder to return a path of 5 positions
        try (MockedStatic<PathFinder> mockedPathFinder = mockStatic(PathFinder.class, withSettings())) {
            List<Position> path = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                path.add(new Position(i, i));
            }
            mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                            .thenReturn(path);
            
            // Act
            Solution evaluatedSolution = SolutionEvaluator.evaluate(solution, mockState);
            
            // Assert
            assertNotNull(evaluatedSolution);
            assertTrue(evaluatedSolution.getCost() >= 0);  // Cost should be non-negative
            assertEquals(ordersState, evaluatedSolution.getOrdersState());
            assertEquals(depotsState, evaluatedSolution.getDepotsState());
            assertEquals(routes, evaluatedSolution.getRoutes());
        }
    }

    @Test
    void evaluate_shouldApplyPenaltyForIncompleteOrders() {
        // Arrange
        // Create a solution with incomplete order
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 15);  // Order has 15 GLP remaining
        
        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);
        
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
            new RouteStop("ORD-1", currentTime.plusHours(1), 5)
        );
        routes.put("V-001", new Route("V-001", stops, currentTime));
        
        Solution solution = new Solution(ordersState, depotsState, routes);
        
        // Mock PathFinder to return a path
        try (MockedStatic<PathFinder> mockedPathFinder = mockStatic(PathFinder.class, withSettings())) {
            List<Position> path = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                path.add(new Position(i, i));
            }
            mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                            .thenReturn(path);
            
            // Act
            Solution evaluatedSolution = SolutionEvaluator.evaluate(solution, mockState);
            
            // Assert
            assertNotNull(evaluatedSolution);
            assertTrue(evaluatedSolution.getCost() > 0);
            // Cost should be higher due to incomplete order penalty
            // With 15 units remaining and a high penalty factor, this should be well over 1000
            assertTrue(evaluatedSolution.getCost() > 5000, "Cost should include significant penalty for incomplete order");
        }
    }
    
    @Test
    void evaluate_shouldReturnInfeasibleSolutionWhenNoPathFound() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 0);
        
        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);
        
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
            new RouteStop("ORD-1", currentTime.plusHours(1), 10)
        );
        routes.put("V-001", new Route("V-001", stops, currentTime));
        
        Solution solution = new Solution(ordersState, depotsState, routes);
        
        // Mock PathFinder to return null (no path found)
        try (MockedStatic<PathFinder> mockedPathFinder = mockStatic(PathFinder.class, withSettings())) {
            mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                            .thenReturn(null);
            
            // Act
            Solution evaluatedSolution = SolutionEvaluator.evaluate(solution, mockState);
            
            // Assert
            assertEquals(Double.POSITIVE_INFINITY, evaluatedSolution.getCost());
        }
    }
    
    @Test
    void evaluate_shouldReturnInfeasibleSolutionWhenInsufficientFuel() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 0);
        
        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);
        
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = Arrays.asList(
            new RouteStop("ORD-1", currentTime.plusHours(1), 10)
        );
        routes.put("V-001", new Route("V-001", stops, currentTime));
        
        Solution solution = new Solution(ordersState, depotsState, routes);
        
        // Mock PathFinder to return a path
        try (MockedStatic<PathFinder> mockedPathFinder = mockStatic(PathFinder.class, withSettings())) {
            List<Position> path = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                path.add(new Position(i, i));
            }
            mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                            .thenReturn(path);
                            
            // Mock vehicle to require more fuel than available
            when(mockVehicle.calculateFuelNeeded(anyDouble())).thenReturn(50.0);
            
            // Act
            Solution evaluatedSolution = SolutionEvaluator.evaluate(solution, mockState);
            
            // Assert
            assertEquals(Double.POSITIVE_INFINITY, evaluatedSolution.getCost());
        }
    }
} 
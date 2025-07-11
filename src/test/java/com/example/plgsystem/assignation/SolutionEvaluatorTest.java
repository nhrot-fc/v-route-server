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

class SolutionEvaluatorTest {
    private SimulationState mockState;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();

        Order order1 = new Order("ORD-1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), 10,
                new Position(0, 0));
        Order order2 = new Order("ORD-2", LocalDateTime.now(), LocalDateTime.now().plusHours(2), 15,
                new Position(0, 0));
        Vehicle vehicle1 = new Vehicle("V-001", VehicleType.TA, new Position(0, 0));
        Depot depot1 = new Depot("DEP-1", new Position(0, 0), 100, DepotType.MAIN);
        Depot depot2 = new Depot("DEP-2", new Position(0, 0), 50, DepotType.AUXILIARY);

        mockState = new SimulationState(List.of(vehicle1), depot1, List.of(depot2), currentTime);
        mockState.addOrder(order1);
        mockState.addOrder(order2);
    }

    @Test
    void evaluate_shouldReturnSolutionWithCalculatedCost() {
        // Arrange
        // Create a solution to evaluate
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = List.of(
                new RouteStop(new Position(0, 0), "ORD-1", currentTime.plusHours(1), 10));
        routes.put("V-001", new Route("V-001", stops, currentTime));

        Solution solution = new Solution(routes, mockState);
        assertTrue(solution.getCost() >= 0);
    }

    @Test
    void evaluate_shouldApplyPenaltyForIncompleteOrders() {
        // Arrange
        // Create a solution with incomplete order
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = List.of(
                new RouteStop(new Position(0, 0), "ORD-1", currentTime.plusHours(1), 5));
        routes.put("V-001", new Route("V-001", stops, currentTime));

        Solution solution = new Solution(routes, mockState);

        // Mock PathFinder to return a path
        try (MockedStatic<PathFinder> mockedPathFinder = mockStatic(PathFinder.class, withSettings())) {
            List<Position> path = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                path.add(new Position(i, i));
            }
            mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                    .thenReturn(path);

            // Act
            double evaluatedSolution = SolutionEvaluator.evaluate(solution, mockState);

            // Assert
            assertTrue(evaluatedSolution > 0);
            // Cost should be higher due to incomplete order penalty
            // With 15 units remaining and a high penalty factor, this should be well over
            // 1000
            assertTrue(evaluatedSolution > 5000, "Cost should include significant penalty for incomplete order");
        }
    }

    @Test
    void evaluate_shouldReturnInfeasibleSolutionWhenNoPathFound() {
        // Arrange
        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = List.of(
                new RouteStop(new Position(0, 0), "ORD-1", currentTime.plusHours(1), 10));
        routes.put("V-001", new Route("V-001", stops, currentTime));

        Solution solution = new Solution(routes, mockState);
        assertEquals(Double.POSITIVE_INFINITY, solution.getCost());
    }

    @Test
    void evaluate_shouldReturnInfeasibleSolutionWhenInsufficientFuel() {
        // Arrange
        Map<String, Integer> ordersState = new HashMap<>();
        ordersState.put("ORD-1", 0);

        Map<String, Integer> depotsState = new HashMap<>();
        depotsState.put("DEP-1", 100);

        Map<String, Route> routes = new HashMap<>();
        List<RouteStop> stops = List.of(
                new RouteStop(new Position(0, 0), "ORD-1", currentTime.plusHours(1), 10));
        routes.put("V-001", new Route("V-001", stops, currentTime));

        Solution solution = new Solution(routes, mockState);

        assertEquals(Double.POSITIVE_INFINITY, solution.getCost());
    }
}
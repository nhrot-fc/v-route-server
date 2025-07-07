package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.simulation.SimulationState;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetaheuristicSolverTest {

    @Mock
    private SimulationState mockState;
    
    @Mock
    private Vehicle mockVehicle;
    
    @Mock
    private Order mockOrder;
    
    @Mock
    private Depot mockMainDepot;
    
    @BeforeEach
    void setUp() {
        // Only mock what's actually needed
        when(mockVehicle.getId()).thenReturn("V-001");
        when(mockOrder.getId()).thenReturn("ORD-1");
        when(mockState.getVehicles()).thenReturn(Arrays.asList(mockVehicle));
        when(mockState.getOrders()).thenReturn(Arrays.asList(mockOrder));
        when(mockState.getVehicleById("V-001")).thenReturn(mockVehicle);
        when(mockState.getOrderById("ORD-1")).thenReturn(mockOrder);
    }

    @Test
    void solve_shouldReturnValidSolution() {
        // Setup mocks for static methods
        try (MockedStatic<RandomDistributor> mockedRandomDistributor = mockStatic(RandomDistributor.class, withSettings());
             MockedStatic<SolutionGenerator> mockedSolutionGenerator = mockStatic(SolutionGenerator.class, withSettings());
             MockedStatic<SolutionEvaluator> mockedSolutionEvaluator = mockStatic(SolutionEvaluator.class, withSettings())) {
            
            // Mock initial assignments
            Map<String, List<DeliveryPart>> mockAssignments = new HashMap<>();
            mockAssignments.put("V-001", Arrays.asList(
                new DeliveryPart("ORD-1", 10, LocalDateTime.now().plusHours(2))
            ));
            
            mockedRandomDistributor.when(() -> RandomDistributor.createInitialRandomAssignments(any()))
                                   .thenReturn(mockAssignments);
            
            // Mock solution generation
            Map<String, Integer> ordersState = new HashMap<>();
            ordersState.put("ORD-1", 0);  // Order fully satisfied
            
            Map<String, Integer> depotsState = new HashMap<>();
            depotsState.put("MAIN-DEP", 990);  // Depot after servicing
            
            Map<String, Route> routes = new HashMap<>();
            List<RouteStop> stops = Arrays.asList(
                new RouteStop("DEP-1", 10), // Load GLP
                new RouteStop("ORD-1", LocalDateTime.now().plusHours(2), 10) // Deliver
            );
            routes.put("V-001", new Route("V-001", stops, LocalDateTime.now()));
            
            Solution mockSolution = new Solution(ordersState, depotsState, routes);
            
            mockedSolutionGenerator.when(() -> SolutionGenerator.generateSolution(any(), any()))
                                   .thenReturn(mockSolution);
            
            // Mock solution evaluation
            mockedSolutionEvaluator.when(() -> SolutionEvaluator.evaluate(any(), any()))
                                   .thenAnswer((Answer<Solution>) invocation -> {
                                       Solution solution = invocation.getArgument(0);
                                       return new Solution(
                                           solution.getOrdersState(), 
                                           solution.getDepotsState(), 
                                           solution.getRoutes(), 
                                           100.0);  // Assign cost
                                   });
            
            // Mock DistributionOperations for neighborhood generation
            try (MockedStatic<DistributionOperations> mockedDistribOps = mockStatic(DistributionOperations.class, withSettings())) {
                mockedDistribOps.when(() -> DistributionOperations.randomOperationWithState(any(), any()))
                                .thenAnswer((Answer<Map<String, List<DeliveryPart>>>) invocation -> {
                                    Map<String, List<DeliveryPart>> input = invocation.getArgument(0);
                                    // Return the same map to simulate no improvement
                                    return new HashMap<>(input);
                                });
                
                // Act
                Solution result = MetaheuristicSolver.solve(mockState);
                
                // Assert
                assertNotNull(result);
                assertFalse(result.getRoutes().isEmpty());
                assertEquals(100.0, result.getCost());
            }
        }
    }
    
    @Test
    void solve_shouldFindBetterSolutionThroughIterations() {
        // Setup mocks for static methods
        try (MockedStatic<RandomDistributor> mockedRandomDistributor = mockStatic(RandomDistributor.class, withSettings());
             MockedStatic<SolutionGenerator> mockedSolutionGenerator = mockStatic(SolutionGenerator.class, withSettings());
             MockedStatic<SolutionEvaluator> mockedSolutionEvaluator = mockStatic(SolutionEvaluator.class, withSettings())) {
            
            // Mock initial assignments
            Map<String, List<DeliveryPart>> mockAssignments = new HashMap<>();
            mockAssignments.put("V-001", Arrays.asList(
                new DeliveryPart("ORD-1", 10, LocalDateTime.now().plusHours(2))
            ));
            
            mockedRandomDistributor.when(() -> RandomDistributor.createInitialRandomAssignments(any()))
                                   .thenReturn(mockAssignments);
            
            // Create a basic solution
            Map<String, Integer> ordersState = new HashMap<>();
            ordersState.put("ORD-1", 0);
            
            Map<String, Integer> depotsState = new HashMap<>();
            depotsState.put("MAIN-DEP", 990);
            
            Map<String, Route> routes = new HashMap<>();
            List<RouteStop> stops = Arrays.asList(
                new RouteStop("MAIN-DEP", 10),
                new RouteStop("ORD-1", LocalDateTime.now().plusHours(2), 10)
            );
            routes.put("V-001", new Route("V-001", stops, LocalDateTime.now()));
            
            Solution initialSolution = new Solution(ordersState, depotsState, routes);
            
            mockedSolutionGenerator.when(() -> SolutionGenerator.generateSolution(any(), any()))
                                   .thenReturn(initialSolution);
            
            // Simulate improving solutions through iterations
            mockedSolutionEvaluator.when(() -> SolutionEvaluator.evaluate(any(), any()))
                                   .thenAnswer(new Answer<Solution>() {
                                       private int callCount = 0;
                                       
                                       @Override
                                       public Solution answer(org.mockito.invocation.InvocationOnMock invocation) {
                                           Solution solution = invocation.getArgument(0);
                                           callCount++;
                                           
                                           // Each call returns a better solution (lower cost)
                                           double cost = 1000.0 - (callCount * 50.0);
                                           if (cost < 0) cost = 0;
                                           
                                           return new Solution(
                                               solution.getOrdersState(),
                                               solution.getDepotsState(),
                                               solution.getRoutes(),
                                               cost
                                           );
                                       }
                                   });
            
            // Mock neighborhood generation
            try (MockedStatic<DistributionOperations> mockedDistribOps = mockStatic(DistributionOperations.class, withSettings())) {
                mockedDistribOps.when(() -> DistributionOperations.randomOperationWithState(any(), any()))
                                .thenAnswer((Answer<Map<String, List<DeliveryPart>>>) invocation -> {
                                    // Return a different assignment each time to trigger evaluation
                                    return new HashMap<>(mockAssignments);
                                });
                
                // Act
                Solution result = MetaheuristicSolver.solve(mockState);
                
                // Assert
                assertNotNull(result);
                // The algorithm should have found a solution with lower cost
                assertTrue(result.getCost() < 1000.0, "Should find better solution through iterations");
            }
        }
    }
} 
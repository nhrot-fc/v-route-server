package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
class SolutionGeneratorTest {

    @Mock
    private SimulationState mockState;
    
    @Mock
    private Vehicle mockVehicle1;
    
    @Mock
    private Vehicle mockVehicle2;
    
    @Mock
    private Order mockOrder1;
    
    @Mock
    private Order mockOrder2;
    
    @Mock
    private Depot mockMainDepot;
    
    @Mock
    private Depot mockAuxDepot;

    private Map<String, List<DeliveryPart>> assignments;
    private List<Position> mockPath;

    @BeforeEach
    void setUp() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Create a mock path that will be returned by PathFinder
        mockPath = new ArrayList<>();
        mockPath.add(new Position(0, 0));
        mockPath.add(new Position(5, 5));
        mockPath.add(new Position(10, 10));
        
        // Setup mock vehicles with stubs
        when(mockVehicle1.getId()).thenReturn("V-001");
        when(mockVehicle1.getCurrentGlpM3()).thenReturn(10);
        when(mockVehicle1.getCurrentFuelGal()).thenReturn(40.0);
        when(mockVehicle1.getFuelCapacityGal()).thenReturn(50.0);
        when(mockVehicle1.getGlpCapacityM3()).thenReturn(30);
        when(mockVehicle1.calculateFuelNeeded(anyDouble())).thenReturn(5.0);
        when(mockVehicle1.getCurrentPosition()).thenReturn(new Position(0, 0));
        
        when(mockVehicle2.getId()).thenReturn("V-002");
        when(mockVehicle2.getCurrentGlpM3()).thenReturn(5);
        when(mockVehicle2.getCurrentFuelGal()).thenReturn(30.0);
        when(mockVehicle2.getFuelCapacityGal()).thenReturn(40.0);
        when(mockVehicle2.getGlpCapacityM3()).thenReturn(20);
        when(mockVehicle2.calculateFuelNeeded(anyDouble())).thenReturn(3.0);
        when(mockVehicle2.getCurrentPosition()).thenReturn(new Position(5, 5));
        
        // Setup mock orders with stubs
        when(mockOrder1.getId()).thenReturn("ORD-1");
        when(mockOrder1.getPosition()).thenReturn(new Position(10, 10));
        when(mockOrder1.getRemainingGlpM3()).thenReturn(8);
        when(mockOrder1.isDelivered()).thenReturn(false);
        when(mockOrder1.getDeadlineTime()).thenReturn(currentTime.plusHours(2));
        
        when(mockOrder2.getId()).thenReturn("ORD-2");
        when(mockOrder2.getPosition()).thenReturn(new Position(15, 15));
        when(mockOrder2.getRemainingGlpM3()).thenReturn(12);
        when(mockOrder2.isDelivered()).thenReturn(false);
        when(mockOrder2.getDeadlineTime()).thenReturn(currentTime.plusHours(3));
        
        // Setup mock depots with stubs
        when(mockMainDepot.getId()).thenReturn("MAIN-DEP");
        when(mockMainDepot.getCurrentGlpM3()).thenReturn(1000);
        when(mockMainDepot.getPosition()).thenReturn(new Position(0, 0));
        when(mockMainDepot.isMain()).thenReturn(true);
        
        when(mockAuxDepot.getId()).thenReturn("AUX-DEP");
        when(mockAuxDepot.getCurrentGlpM3()).thenReturn(100);
        when(mockAuxDepot.getPosition()).thenReturn(new Position(20, 20));
        when(mockAuxDepot.isMain()).thenReturn(false);
        
        // Setup mock state with stubs
        when(mockState.getCurrentTime()).thenReturn(currentTime);
        when(mockState.getVehicleById("V-001")).thenReturn(mockVehicle1);
        when(mockState.getVehicleById("V-002")).thenReturn(mockVehicle2);
        when(mockState.getOrderById("ORD-1")).thenReturn(mockOrder1);
        when(mockState.getOrderById("ORD-2")).thenReturn(mockOrder2);
        when(mockState.getMainDepot()).thenReturn(mockMainDepot);
        when(mockState.getAuxDepots()).thenReturn(List.of(mockAuxDepot));
        when(mockState.getDepotById("MAIN-DEP")).thenReturn(mockMainDepot);
        when(mockState.getDepotById("AUX-DEP")).thenReturn(mockAuxDepot);
        when(mockState.isPositionBlockedAt(any(), any())).thenReturn(false);
        
        // This is the crucial mock that was missing - SolutionGenerator uses state.getOrders() to initialize ordersState
        when(mockState.getOrders()).thenReturn(Arrays.asList(mockOrder1, mockOrder2));
        
        // Create assignments for test
        assignments = new HashMap<>();
        assignments.put("V-001", new ArrayList<>(Arrays.asList(
            new DeliveryPart("ORD-1", 5, currentTime.plusHours(2)),
            new DeliveryPart("ORD-2", 7, currentTime.plusHours(3))
        )));
        assignments.put("V-002", new ArrayList<>(List.of(
                new DeliveryPart("ORD-2", 5, currentTime.plusHours(3))
        )));
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
            assertNotNull(solution.getOrdersState());
            assertNotNull(solution.getDepotsState());
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
            Map<String, Integer> ordersState = solution.getOrdersState();
            Map<String, Integer> depotsState = solution.getDepotsState();
            
            assertNotNull(ordersState);
            assertNotNull(depotsState);
            
            // Check that orders state contains both orders
            assertTrue(ordersState.containsKey("ORD-1"));
            assertTrue(ordersState.containsKey("ORD-2"));
            
            // Check that depots state contains both depots
            assertTrue(depotsState.containsKey("MAIN-DEP"));
            assertTrue(depotsState.containsKey("AUX-DEP"));
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
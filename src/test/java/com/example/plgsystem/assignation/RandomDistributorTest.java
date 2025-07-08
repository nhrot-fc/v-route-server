package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

class RandomDistributorTest {

    @Mock
    private SimulationState mockState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createInitialRandomAssignments_shouldReturnEmptyAssignments_whenNoOrders() {
        // Arrange
        List<Vehicle> vehicles = Arrays.asList(
            createMockVehicle("V-001", VehicleType.TA, true, 30),
            createMockVehicle("V-002", VehicleType.TB, true, 25)
        );
        
        when(mockState.getVehicles()).thenReturn(vehicles);
        when(mockState.getOrders()).thenReturn(List.of());
        
        // Act
        Map<String, List<DeliveryPart>> result = RandomDistributor.createInitialRandomAssignments(mockState);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("V-001"));
        assertTrue(result.containsKey("V-002"));
        assertTrue(result.get("V-001").isEmpty());
        assertTrue(result.get("V-002").isEmpty());
    }
    
    @Test
    void createInitialRandomAssignments_shouldDistributeOrders() {
        // Arrange
        List<Vehicle> vehicles = Arrays.asList(
            createMockVehicle("V-001", VehicleType.TA, true, 30),
            createMockVehicle("V-002", VehicleType.TB, true, 20)
        );
        
        List<Order> orders = Arrays.asList(
            createMockOrder("ORD-1", 8, false),
            createMockOrder("ORD-2", 12, false)
        );
        
        when(mockState.getVehicles()).thenReturn(vehicles);
        when(mockState.getOrders()).thenReturn(orders);
        
        // Act
        Map<String, List<DeliveryPart>> result = RandomDistributor.createInitialRandomAssignments(mockState);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Calculate total GLP assigned across all vehicles
        int totalAssignedGlp = 0;
        for (List<DeliveryPart> vehicleAssignments : result.values()) {
            for (DeliveryPart part : vehicleAssignments) {
                totalAssignedGlp += part.getGlpDeliverM3();
            }
        }
        
        // The total assigned GLP should equal the sum of what's requested in orders
        assertEquals(20, totalAssignedGlp);
    }
    
    @Test
    void createInitialRandomAssignments_shouldNotAssignToUnavailableVehicles() {
        // Arrange
        Vehicle availableVehicle = createMockVehicle("V-001", VehicleType.TA, true, 30);
        Vehicle unavailableVehicle = createMockVehicle("V-002", VehicleType.TB, false, 20);
        
        List<Vehicle> vehicles = Arrays.asList(availableVehicle, unavailableVehicle);
        
        List<Order> orders = List.of(
                createMockOrder("ORD-1", 10, false)
        );
        
        when(mockState.getVehicles()).thenReturn(vehicles);
        when(mockState.getOrders()).thenReturn(orders);
        
        // Act
        Map<String, List<DeliveryPart>> result = RandomDistributor.createInitialRandomAssignments(mockState);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("V-001"));
        assertFalse(result.containsKey("V-002"));
    }
    
    @Test
    void createInitialRandomAssignments_shouldNotAssignDeliveredOrders() {
        // Arrange
        List<Vehicle> vehicles = List.of(
                createMockVehicle("V-001", VehicleType.TA, true, 30)
        );
        
        List<Order> orders = Arrays.asList(
            createMockOrder("ORD-1", 10, false),
            createMockOrder("ORD-2", 15, true) // Already delivered
        );
        
        when(mockState.getVehicles()).thenReturn(vehicles);
        when(mockState.getOrders()).thenReturn(orders);
        
        // Act
        Map<String, List<DeliveryPart>> result = RandomDistributor.createInitialRandomAssignments(mockState);
        
        // Assert
        assertNotNull(result);
        
        // Calculate total GLP assigned
        int totalAssignedGlp = 0;
        for (List<DeliveryPart> vehicleAssignments : result.values()) {
            for (DeliveryPart part : vehicleAssignments) {
                totalAssignedGlp += part.getGlpDeliverM3();
            }
        }
        
        // Only the undelivered order should be assigned
        assertEquals(10, totalAssignedGlp);
    }
    
    // Helper methods
    private Vehicle createMockVehicle(String id, VehicleType type, boolean available, int capacity) {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(id);
        when(vehicle.getType()).thenReturn(type);
        when(vehicle.isAvailable()).thenReturn(available);
        when(vehicle.getGlpCapacityM3()).thenReturn(capacity);
        when(vehicle.getCurrentPosition()).thenReturn(new Position(0, 0));
        return vehicle;
    }
    
    private Order createMockOrder(String id, int glpRequest, boolean delivered) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getGlpRequestM3()).thenReturn(glpRequest);
        when(order.getRemainingGlpM3()).thenReturn(delivered ? 0 : glpRequest);
        when(order.isDelivered()).thenReturn(delivered);
        when(order.getDeadlineTime()).thenReturn(LocalDateTime.now().plusHours(2));
        when(order.getPosition()).thenReturn(new Position(10, 10));
        return order;
    }
} 
package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

class DeliveryOptimizerTest {

    @Mock
    private SimulationState mockState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void optimizeAssignments_shouldApplySplitAndMerge() {
        // Arrange
        Map<String, List<DeliveryPart>> assignments = new HashMap<>();
        
        // Create two vehicle routes
        List<DeliveryPart> vehicle1Deliveries = List.of(
                createDeliveryPart("O1", 12),
                createDeliveryPart("O1", 8),
                createDeliveryPart("O1", 7));

        List<DeliveryPart> vehicle2Deliveries = List.of(
                createDeliveryPart("O3", 9),
                createDeliveryPart("O3", 6));
        
        assignments.put("V1", new ArrayList<>(vehicle1Deliveries));
        assignments.put("V2", new ArrayList<>(vehicle2Deliveries));
        
        // Mock vehicles in state
        Vehicle vehicle1 = mock(Vehicle.class);
        Vehicle vehicle2 = mock(Vehicle.class);
        
        when(vehicle1.getGlpCapacityM3()).thenReturn(15);
        when(vehicle2.getGlpCapacityM3()).thenReturn(10);
        
        when(mockState.getVehicleById("V1")).thenReturn(vehicle1);
        when(mockState.getVehicleById("V2")).thenReturn(vehicle2);
        
        // Act
        Map<String, List<DeliveryPart>> result = DeliveryOptimizer.optimizeAssignments(assignments, mockState);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Vehicle 1 should have chunks of 15 and 12 (5+5+2 from O1, and 5+2 from O2)
        List<DeliveryPart> v1Result = result.get("V1");
        assertEquals(15, v1Result.get(0).getGlpDeliverM3());
        assertEquals("O1", v1Result.get(0).getOrderId());
        assertEquals(12, v1Result.get(1).getGlpDeliverM3());
        assertEquals("O1", v1Result.get(1).getOrderId());
        assertEquals(2, v1Result.size());

        // Vehicle 2 should have chunks of 10 and 5
        List<DeliveryPart> v2Result = result.get("V2");
        assertEquals(10, v2Result.get(0).getGlpDeliverM3());
        assertEquals("O3", v2Result.get(0).getOrderId());
        assertEquals(5, v2Result.get(1).getGlpDeliverM3());
        assertEquals("O3", v2Result.get(1).getOrderId());
        assertEquals(2, v2Result.size());
    }
    
    @Test
    void moveDeliveryWithOptimization_shouldMoveAndOptimize() {
        // Arrange
        Map<String, List<DeliveryPart>> assignments = new HashMap<>();
        
        // Create two vehicle routes
        List<DeliveryPart> vehicle1Deliveries = List.of(
                createDeliveryPart("O1", 7),
                createDeliveryPart("O2", 8));
        
        List<DeliveryPart> vehicle2Deliveries = List.of(
                createDeliveryPart("O3", 6));
        
        assignments.put("V1", new ArrayList<>(vehicle1Deliveries));
        assignments.put("V2", new ArrayList<>(vehicle2Deliveries));
        
        // Mock vehicles in state
        Vehicle vehicle1 = mock(Vehicle.class);
        Vehicle vehicle2 = mock(Vehicle.class);
        
        when(vehicle1.getGlpCapacityM3()).thenReturn(5);
        when(vehicle2.getGlpCapacityM3()).thenReturn(20);
        
        when(mockState.getVehicleById("V1")).thenReturn(vehicle1);
        when(mockState.getVehicleById("V2")).thenReturn(vehicle2);
        
        // Act - Move "O2" from vehicle 1 to vehicle 2
        Map<String, List<DeliveryPart>> result = DeliveryOptimizer.moveDeliveryWithOptimization(
                assignments, "V1", "V2", 1, mockState);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Vehicle 1 should only have O1 left, split into atomic parts
        List<DeliveryPart> v1Result = result.get("V1");
        assertEquals(5, v1Result.get(0).getGlpDeliverM3());
        assertEquals("O1", v1Result.get(0).getOrderId());
        assertEquals(2, v1Result.get(1).getGlpDeliverM3());
        assertEquals("O1", v1Result.get(1).getOrderId());
        assertEquals(2, v1Result.size());
        
        // Vehicle 2 should have O3 and O2, merged optimally
        List<DeliveryPart> v2Result = result.get("V2");
        assertEquals(6, v2Result.get(0).getGlpDeliverM3());
        assertEquals("O3", v2Result.get(0).getOrderId());
        assertEquals(8, v2Result.get(1).getGlpDeliverM3());
        assertEquals("O2", v2Result.get(1).getOrderId());
        assertEquals(2, v2Result.size());
    }

    private DeliveryPart createDeliveryPart(String orderId, int glpAmount) {
        return new DeliveryPart(orderId, glpAmount, LocalDateTime.now().plusHours(2));
    }
}

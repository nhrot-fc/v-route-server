package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;
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

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributionOperationsTest {
    
    @Mock
    private SimulationState mockState;
    
    private Map<String, List<DeliveryPart>> assignments;
    private LocalDateTime currentTime;
    
    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        
        // Setup initial assignments for testing
        assignments = new HashMap<>();
        assignments.put("V-001", new ArrayList<>(Arrays.asList(
            new DeliveryPart("ORD-1", 5, currentTime.plusHours(1)),
            new DeliveryPart("ORD-2", 10, currentTime.plusHours(2))
        )));
        assignments.put("V-002", new ArrayList<>(Arrays.asList(
            new DeliveryPart("ORD-3", 7, currentTime.plusHours(3)),
            new DeliveryPart("ORD-4", 3, currentTime.plusHours(4))
        )));
        
        // Mock orders with positions for geographic clustering
        Order order1 = mock(Order.class);
        when(order1.getId()).thenReturn("ORD-1");
        when(order1.getPosition()).thenReturn(new Position(10, 10));
        
        Order order2 = mock(Order.class);
        when(order2.getId()).thenReturn("ORD-2");
        when(order2.getPosition()).thenReturn(new Position(15, 15));
        
        Order order3 = mock(Order.class);
        when(order3.getId()).thenReturn("ORD-3");
        when(order3.getPosition()).thenReturn(new Position(30, 30));
        
        Order order4 = mock(Order.class);
        when(order4.getId()).thenReturn("ORD-4");
        when(order4.getPosition()).thenReturn(new Position(35, 35));
        
        // Setup state mock with stubs
        when(mockState.getOrderById("ORD-1")).thenReturn(order1);
        when(mockState.getOrderById("ORD-2")).thenReturn(order2);
        when(mockState.getOrderById("ORD-3")).thenReturn(order3);
        when(mockState.getOrderById("ORD-4")).thenReturn(order4);
        
        // Setup vehicles with different capacities for balancing tests
        Vehicle vehicle1 = mock(Vehicle.class);
        when(vehicle1.getId()).thenReturn("V-001");
        when(vehicle1.getGlpCapacityM3()).thenReturn(30);
        
        Vehicle vehicle2 = mock(Vehicle.class);
        when(vehicle2.getId()).thenReturn("V-002");
        when(vehicle2.getGlpCapacityM3()).thenReturn(20);
        
        when(mockState.getVehicleById("V-001")).thenReturn(vehicle1);
        when(mockState.getVehicleById("V-002")).thenReturn(vehicle2);
    }
    
    @Test
    void cloneAssignments_shouldCreateDeepCopy() {
        // Act
        Map<String, List<DeliveryPart>> clone = DistributionOperations.cloneAssignments(assignments);
        
        // Assert
        assertNotSame(assignments, clone);
        assertEquals(assignments.size(), clone.size());
        
        // Check values are equal but not the same objects
        for (String key : assignments.keySet()) {
            assertNotSame(assignments.get(key), clone.get(key));
            assertEquals(assignments.get(key).size(), clone.get(key).size());
        }
        
        // Verify modifying clone doesn't affect original
        clone.get("V-001").add(new DeliveryPart("ORD-5", 2, currentTime.plusHours(5)));
        assertEquals(2, assignments.get("V-001").size());
        assertEquals(3, clone.get("V-001").size());
    }
    
    @Test
    void moveDeliveryBetweenVehicles_shouldMoveDelivery() {
        // Act
        Map<String, List<DeliveryPart>> result = DistributionOperations.moveDeliveryBetweenVehicles(assignments, mockState);
        
        // Assert
        int originalTotal = assignments.values().stream()
            .mapToInt(List::size).sum();
        
        int resultTotal = result.values().stream()
            .mapToInt(List::size).sum();
        
        // Total number of deliveries should remain the same
        assertEquals(originalTotal, resultTotal);
        
        // But distribution among vehicles should be different
        boolean isDifferent = !assignments.equals(result);
        assertTrue(isDifferent, "Assignment should be modified");
    }
    
    @Test
    void sortByDeadline_shouldSortDeliveries() {
        // Act
        Map<String, List<DeliveryPart>> sorted = DistributionOperations.sortByDeadline(assignments, mockState);
        
        // Assert
        for (String vehicleId : sorted.keySet()) {
            List<DeliveryPart> deliveries = sorted.get(vehicleId);
            
            // Verify deliveries are sorted by deadline
            if (deliveries.size() > 1) {
                for (int i = 0; i < deliveries.size() - 1; i++) {
                    LocalDateTime current = deliveries.get(i).getDeadlineTime();
                    LocalDateTime next = deliveries.get(i+1).getDeadlineTime();
                    assertTrue(current.isBefore(next) || current.isEqual(next));
                }
            }
        }
    }
    
    @Test
    void balanceByCapacity_shouldRebalanceBasedOnCapacity() {
        // Act
        Map<String, List<DeliveryPart>> balanced = DistributionOperations.balanceByCapacity(assignments, mockState);
        
        // Assert
        // Calculate GLP assignments per vehicle
        int vehicle1Glp = balanced.get("V-001").stream().mapToInt(DeliveryPart::getGlpDeliverM3).sum();
        int vehicle2Glp = balanced.get("V-002").stream().mapToInt(DeliveryPart::getGlpDeliverM3).sum();
        
        // Check that the vehicle with larger capacity has more or equal GLP assigned
        assertTrue(vehicle1Glp >= vehicle2Glp, "Vehicle with larger capacity should have more GLP assigned");
        
        // Total GLP should remain the same
        int originalGlp = assignments.values().stream()
            .flatMap(List::stream)
            .mapToInt(DeliveryPart::getGlpDeliverM3)
            .sum();
        
        int balancedGlp = balanced.values().stream()
            .flatMap(List::stream)
            .mapToInt(DeliveryPart::getGlpDeliverM3)
            .sum();
        
        assertEquals(originalGlp, balancedGlp);
    }
    
    @Test
    void randomOperationWithState_shouldReturnModifiedAssignments() {
        // Act
        Map<String, List<DeliveryPart>> result = DistributionOperations.randomOperationWithState(assignments, mockState);
        
        // Assert
        assertNotNull(result);
        
        // Result should have the same vehicles
        assertEquals(assignments.keySet(), result.keySet());
        
        // The modification should preserve the total number of deliveries
        int originalCount = assignments.values().stream().mapToInt(List::size).sum();
        int resultCount = result.values().stream().mapToInt(List::size).sum();
        assertEquals(originalCount, resultCount);
        
        // The total GLP should also be preserved
        int originalGlp = assignments.values().stream()
            .flatMap(List::stream)
            .mapToInt(DeliveryPart::getGlpDeliverM3)
            .sum();
        
        int resultGlp = result.values().stream()
            .flatMap(List::stream)
            .mapToInt(DeliveryPart::getGlpDeliverM3)
            .sum();
        
        assertEquals(originalGlp, resultGlp);
    }
} 
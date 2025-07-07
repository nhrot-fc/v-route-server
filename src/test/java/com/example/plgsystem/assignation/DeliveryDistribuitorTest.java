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

class DeliveryDistribuitorTest {

    @Mock
    private SimulationState mockEnvironment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createInitialRandomAssignments_shouldReturnEmptySolution_whenNoPendingOrders() {
        // Arrange
        List<Vehicle> availableVehicles = Arrays.asList(
                createVehicle("TA01", VehicleType.TA, 30),
                createVehicle("TB01", VehicleType.TB, 20));

        when(mockEnvironment.getVehicles()).thenReturn(availableVehicles);
        when(mockEnvironment.getOrders()).thenReturn(List.of());

        // Act
        Map<String, List<DeliveryPart>> solution = RandomDistributor.createInitialRandomAssignments(mockEnvironment);

        // Assert
        assertNotNull(solution);
        assertEquals(2, solution.size());
        assertTrue(solution.get(availableVehicles.get(0).getId()).isEmpty());
        assertTrue(solution.get(availableVehicles.get(1).getId()).isEmpty());
    }

    @Test
    void createInitialRandomAssignments_shouldReturnEmptySolution_whenNoAvailableVehicles() {
        // Arrange
        List<Order> pendingOrders = Arrays.asList(
                createOrder("ORD-1", 15),
                createOrder("ORD-2", 20));

        when(mockEnvironment.getVehicles()).thenReturn(List.of());
        when(mockEnvironment.getOrders()).thenReturn(pendingOrders);

        // Act
        Map<String, List<DeliveryPart>> solution = RandomDistributor.createInitialRandomAssignments(mockEnvironment);

        // Assert
        assertNotNull(solution);
        assertTrue(solution.isEmpty());
    }

    @Test
    void createInitialRandomAssignments_shouldDistributeOrdersIntoPackages() {
        // Arrange
        Vehicle vehicle1 = createVehicle("TA01", VehicleType.TA, 30);
        Vehicle vehicle2 = createVehicle("TB01", VehicleType.TB, 20);

        List<Vehicle> availableVehicles = Arrays.asList(vehicle1, vehicle2);

        Order order1 = createOrder("ORD-1", 7); // Will create 2 packages (5 + 2)
        Order order2 = createOrder("ORD-2", 12); // Will create 3 packages (5 + 5 + 2)

        List<Order> pendingOrders = Arrays.asList(order1, order2);

        when(mockEnvironment.getVehicles()).thenReturn(availableVehicles);
        when(mockEnvironment.getOrders()).thenReturn(pendingOrders);

        // Act
        Map<String, List<DeliveryPart>> solution = RandomDistributor.createInitialRandomAssignments(mockEnvironment);

        // Assert
        assertNotNull(solution);
        assertEquals(2, solution.size());

        // Total package count should be 5 (2 from order1 + 3 from order2)
        int totalPackages = solution.values().stream()
                .mapToInt(List::size)
                .sum();
        assertEquals(5, totalPackages);

        // Check that all GLP from orders is assigned
        int assignedGlp = 0;
        for (List<DeliveryPart> instructions : solution.values()) {
            for (DeliveryPart instruction : instructions) {
                assignedGlp += instruction.getGlpDeliverM3();
            }
        }
        assertEquals(order1.getGlpRequestM3() + order2.getGlpRequestM3(), assignedGlp);
        assertEquals(order1.getGlpRequestM3() + order2.getGlpRequestM3(), assignedGlp);
    }

    @Test
    void createInitialRandomAssignments_shouldPreferVehiclesWithLargerCapacity() {
        // Arrange - create vehicles with significantly different capacities
        Vehicle largeVehicle = createVehicle("TA01", VehicleType.TA, 100);
        Vehicle smallVehicle = createVehicle("TD01", VehicleType.TD, 10);

        List<Vehicle> availableVehicles = Arrays.asList(largeVehicle, smallVehicle);

        // Create a large number of small packages to see the statistical distribution
        Order largeOrder = createOrder("ORD-1", 500); // Will create 100 packages of 5 GLP each

        List<Order> pendingOrders = Arrays.asList(largeOrder);

        when(mockEnvironment.getVehicles()).thenReturn(availableVehicles);
        when(mockEnvironment.getOrders()).thenReturn(pendingOrders);

        // Act
        Map<String, List<DeliveryPart>> solution = RandomDistributor.createInitialRandomAssignments(mockEnvironment);

        // Assert
        Map<String, List<DeliveryPart>> assignments = solution;

        // The test might be probabilistic, but with such a large difference in
        // capacity,
        // we expect the larger vehicle to get significantly more packages
        // (approximately 10x more since capacity ratio is 10:1)
        int largeVehiclePackages = assignments.get(largeVehicle.getId()).size();
        int smallVehiclePackages = assignments.get(smallVehicle.getId()).size();

        // We can't assert exact counts due to randomness, but the larger vehicle should
        // get more
        assertTrue(largeVehiclePackages > smallVehiclePackages,
                "Large vehicle (" + largeVehiclePackages + " packages) should have more packages than small vehicle ("
                        + smallVehiclePackages + " packages)");
    }

    private Vehicle createVehicle(String id, VehicleType type, int glpCapacity) {
        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .type(type)
                .currentPosition(new Position(0, 0))
                .build();
        // Using reflection to set the capacity since it's normally derived from the
        // type
        try {
            java.lang.reflect.Field field = Vehicle.class.getDeclaredField("glpCapacityM3");
            field.setAccessible(true);
            field.set(vehicle, glpCapacity);
        } catch (Exception e) {
            fail("Could not set glpCapacityM3: " + e.getMessage());
        }
        return vehicle;
    }

    private Order createOrder(String id, int glpRequest) {
        return Order.builder()
                .id(id)
                .arrivalTime(LocalDateTime.now().minusHours(1))
                .deadlineTime(LocalDateTime.now().plusHours(2))
                .glpRequestM3(glpRequest)
                .position(new Position(10, 10))
                .build();
    }
}
package com.example.plgsystem.controller;

import com.example.plgsystem.model.*;
import com.example.plgsystem.repository.*;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.enums.DepotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DepotRepository depotRepository;

    @Mock
    private BlockageRepository blockageRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDashboardOverview_shouldReturnCompleteOverview() {
        // Arrange
        Position position = new Position(10, 10);
        
        // Mock vehicles
        Vehicle availableVehicle = Vehicle.builder()
                .id("TA01")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();
                
        Vehicle maintenanceVehicle = Vehicle.builder()
                .id("TB01")
                .type(VehicleType.TB)
                .currentPosition(position)
                .build();
        maintenanceVehicle.setStatus(VehicleStatus.MAINTENANCE);
        
        Vehicle incidentVehicle = Vehicle.builder()
                .id("TC01")
                .type(VehicleType.TC)
                .currentPosition(position)
                .build();
        incidentVehicle.setStatus(VehicleStatus.INCIDENT);
        
        List<Vehicle> allVehicles = Arrays.asList(availableVehicle, maintenanceVehicle, incidentVehicle);
        
        // Mock orders
        Order pendingOrder = Order.builder()
                .id("ORD-1")
                .arrivalTime(LocalDateTime.now().minusHours(2))
                .deadlineTime(LocalDateTime.now().plusHours(2))
                .glpRequestM3(20)
                .position(new Position(20, 20))
                .build();
                
        Order completedOrder = Order.builder()
                .id("ORD-2")
                .arrivalTime(LocalDateTime.now().minusHours(4))
                .deadlineTime(LocalDateTime.now().minusHours(1))
                .glpRequestM3(15)
                .position(new Position(30, 30))
                .build();
        completedOrder.setRemainingGlpM3(0);
        
        // Mock depots
        Depot centralDepot = new Depot("CENTRAL", new Position(0, 0), 1000, DepotType.MAIN);
        centralDepot.setCurrentGlpM3(800);
        
        Depot northDepot = new Depot("NORTH", new Position(50, 50), 160, DepotType.AUXILIARY);
        northDepot.setCurrentGlpM3(120);
        
        // Set up repository mocks
        when(vehicleRepository.count()).thenReturn(3L);
        when(vehicleRepository.findByStatusOrderByCurrentGlpM3Desc(VehicleStatus.AVAILABLE))
                .thenReturn(Collections.singletonList(availableVehicle));
        when(vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE))
                .thenReturn(Collections.singletonList(maintenanceVehicle));
        when(vehicleRepository.findByStatus(VehicleStatus.INCIDENT))
                .thenReturn(Collections.singletonList(incidentVehicle));
        when(vehicleRepository.findAll()).thenReturn(allVehicles);
        
        when(orderRepository.count()).thenReturn(2L);
        when(orderRepository.findPendingDeliveries())
                .thenReturn(Collections.singletonList(pendingOrder));
        when(orderRepository.findByRemainingGlpM3(0))
                .thenReturn(Collections.singletonList(completedOrder));
        when(orderRepository.findByDeadlineTimeBefore(any()))
                .thenReturn(Collections.singletonList(completedOrder));
        
        when(depotRepository.findAll())
                .thenReturn(Arrays.asList(centralDepot, northDepot));
        
        when(blockageRepository.findAll()).thenReturn(Collections.emptyList());
        when(maintenanceRepository.findAll()).thenReturn(Collections.emptyList());
        when(incidentRepository.findAll()).thenReturn(Collections.emptyList());
        
        // Act
        Map<String, Object> overview = dashboardController.getDashboardOverview();
        
        // Assert
        assertNotNull(overview);
        assertEquals(3L, overview.get("totalVehicles"));
        assertEquals(1, overview.get("availableVehicles"));
        assertEquals(1, overview.get("vehiclesInMaintenance"));
        assertEquals(1, overview.get("vehiclesWithIncidents"));
        
        assertEquals(2L, overview.get("totalOrders"));
        assertEquals(1, overview.get("pendingOrders"));
        assertEquals(1, overview.get("completedOrders"));
        assertEquals(0, overview.get("overdueOrders"));
        
        assertEquals(1160.0, overview.get("totalStorageCapacity"));
        assertEquals(920.0, overview.get("currentTotalGLP"));
        
        // Check timestamp is present
        assertNotNull(overview.get("timestamp"));
    }

    @Test
    void getVehicleStatusBreakdown_shouldReturnVehiclesByStatus() {
        // Arrange
        Position position = new Position(10, 10);
        
        Vehicle availableVehicle = Vehicle.builder()
                .id("TA01")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();
                
        Vehicle maintenanceVehicle = Vehicle.builder()
                .id("TB01")
                .type(VehicleType.TB)
                .currentPosition(position)
                .build();
        maintenanceVehicle.setStatus(VehicleStatus.MAINTENANCE);
        
        // Set up repository mocks
        when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE))
                .thenReturn(Collections.singletonList(availableVehicle));
        when(vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE))
                .thenReturn(Collections.singletonList(maintenanceVehicle));
        when(vehicleRepository.findByStatus(VehicleStatus.INCIDENT))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.findByStatus(VehicleStatus.DRIVING))
                .thenReturn(Collections.emptyList());
        when(vehicleRepository.findByStatus(VehicleStatus.SERVING))
                .thenReturn(Collections.emptyList());
        
        // Act
        Map<String, List<Vehicle>> vehicleStatus = dashboardController.getVehicleStatusBreakdown();
        
        // Assert
        assertNotNull(vehicleStatus);
        assertEquals(1, vehicleStatus.get("available").size());
        assertEquals(1, vehicleStatus.get("maintenance").size());
        assertEquals(0, vehicleStatus.get("incident").size());
        assertEquals(0, vehicleStatus.get("inRoute").size());
        assertEquals(0, vehicleStatus.get("delivering").size());
    }

    @Test
    void getUrgentOrders_shouldReturnOrdersDueWithinSpecifiedHours() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeHoursFromNow = now.plusHours(3);
        LocalDateTime sixHoursFromNow = now.plusHours(6);
        
        Order urgentOrder1 = Order.builder()
                .id("ORD-1")
                .arrivalTime(now.minusHours(1))
                .deadlineTime(threeHoursFromNow)
                .glpRequestM3(20)
                .position(new Position(20, 20))
                .build();
                
        Order urgentOrder2 = Order.builder()
                .id("ORD-2")
                .arrivalTime(now)
                .deadlineTime(now.plusHours(2))
                .glpRequestM3(15)
                .position(new Position(30, 30))
                .build();
                
        Order nonUrgentOrder = Order.builder()
                .id("ORD-3")
                .arrivalTime(now)
                .deadlineTime(sixHoursFromNow)
                .glpRequestM3(25)
                .position(new Position(40, 40))
                .build();
        
        List<Order> pendingOrders = Arrays.asList(urgentOrder1, urgentOrder2, nonUrgentOrder);
        when(orderRepository.findPendingDeliveries()).thenReturn(pendingOrders);
        
        // Act
        List<Order> urgentOrders = dashboardController.getUrgentOrders(4); // Get orders due within 4 hours
        
        // Assert
        assertEquals(2, urgentOrders.size());
        assertTrue(urgentOrders.contains(urgentOrder1));
        assertTrue(urgentOrders.contains(urgentOrder2));
        assertFalse(urgentOrders.contains(nonUrgentOrder));
    }

    @Test
    void getSystemHealth_shouldCalculateHealthScores() {
        // Arrange
        when(vehicleRepository.count()).thenReturn(10L);
        when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).thenReturn(Arrays.asList(
                Vehicle.builder().id("TA01").type(VehicleType.TA).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TA02").type(VehicleType.TA).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TB01").type(VehicleType.TB).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TB02").type(VehicleType.TB).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TC01").type(VehicleType.TC).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TC02").type(VehicleType.TC).currentPosition(new Position(0,0)).build(),
                Vehicle.builder().id("TD01").type(VehicleType.TD).currentPosition(new Position(0,0)).build()
        )); // 7 available vehicles out of 10
        
        // No active incidents
        when(incidentRepository.findAll()).thenReturn(Collections.emptyList());
        
        // One overdue order that is still pending delivery
        Order overdueOrder = Order.builder()
                .id("ORD-1")
                .arrivalTime(LocalDateTime.now().minusDays(1))
                .deadlineTime(LocalDateTime.now().minusHours(2))
                .glpRequestM3(20)
                .position(new Position(20, 20))
                .build();
                
        // Make sure the order is still pending (has not been fully delivered)
        // By default, remainingGlpM3 is set to glpRequestM3 in the Order builder
        
        when(orderRepository.findByDeadlineTimeBefore(any())).thenReturn(Collections.singletonList(overdueOrder));
        
        // Act
        Map<String, Object> health = dashboardController.getSystemHealth();
        
        // Assert
        assertNotNull(health);
        assertEquals(70L, health.get("vehicleHealthScore"));  // 7/10 * 100
        assertEquals(100L, health.get("incidentHealthScore")); // No incidents
        assertEquals(95L, health.get("orderHealthScore"));    // One overdue order, 100 - 5
        assertEquals(88L, health.get("overallHealthScore"));  // Average of above
        assertEquals("GOOD", health.get("status"));
    }
} 
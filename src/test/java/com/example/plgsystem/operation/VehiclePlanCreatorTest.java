package com.example.plgsystem.operation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.RouteStop;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.pathfinding.PathFinder;
import com.example.plgsystem.simulation.SimulationState;

class VehiclePlanCreatorTest {

    @Mock
    private SimulationState mockState;

    @Mock
    private Order testOrder1;

    @Mock
    private Order testOrder2;

    @Mock
    private Depot mainDepot;

    @Mock
    private Depot auxiliaryDepot;

    private Vehicle testVehicle;
    private Position vehiclePosition;
    private Position orderPosition1;
    private Position orderPosition2;
    private Position mainDepotPosition;
    private Position auxDepotPosition;
    private LocalDateTime startTime;
    private MockedStatic<PathFinder> mockedPathFinder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create positions
        vehiclePosition = new Position(0, 0);
        orderPosition1 = new Position(10, 10);
        orderPosition2 = new Position(20, 20);
        mainDepotPosition = new Position(5, 5);
        auxDepotPosition = new Position(15, 15);

        // Create vehicle using Mockito
        testVehicle = mock(Vehicle.class);
        when(testVehicle.getId()).thenReturn("V001");
        when(testVehicle.getCurrentPosition()).thenReturn(vehiclePosition);
        when(testVehicle.getCurrentFuelGal()).thenReturn(100.0);
        when(testVehicle.getCurrentGlpM3()).thenReturn(30);
        when(testVehicle.getGlpCapacityM3()).thenReturn(30);
        when(testVehicle.getFuelCapacityGal()).thenReturn(100.0);
        when(testVehicle.getType()).thenReturn(VehicleType.TA);
        when(testVehicle.getStatus()).thenReturn(VehicleStatus.AVAILABLE);

        // Configure mock orders
        when(testOrder1.getId()).thenReturn("ORD001");
        when(testOrder1.getPosition()).thenReturn(orderPosition1);
        when(testOrder1.getDeadlineTime()).thenReturn(LocalDateTime.now().plusDays(1));

        when(testOrder2.getId()).thenReturn("ORD002");
        when(testOrder2.getPosition()).thenReturn(orderPosition2);
        when(testOrder2.getDeadlineTime()).thenReturn(LocalDateTime.now().plusDays(2));

        // Configure mock depots
        when(mainDepot.getId()).thenReturn("DEP001");
        when(mainDepot.getPosition()).thenReturn(mainDepotPosition);
        when(mainDepot.getCurrentGlpM3()).thenReturn(10000);
        when(mainDepot.getType()).thenReturn(DepotType.MAIN);
        when(mainDepot.isMain()).thenReturn(true);
        when(mainDepot.isAuxiliary()).thenReturn(false);
        when(mainDepot.canServe(anyInt())).thenReturn(true);

        when(auxiliaryDepot.getId()).thenReturn("DEP002");
        when(auxiliaryDepot.getPosition()).thenReturn(auxDepotPosition);
        when(auxiliaryDepot.getCurrentGlpM3()).thenReturn(5000);
        when(auxiliaryDepot.getType()).thenReturn(DepotType.AUXILIARY);
        when(auxiliaryDepot.isMain()).thenReturn(false);
        when(auxiliaryDepot.isAuxiliary()).thenReturn(true);
        when(auxiliaryDepot.canServe(anyInt())).thenReturn(true);

        // Configure simulation state
        when(mockState.getCurrentTime()).thenReturn(startTime);
        when(mockState.getVehicleById("V001")).thenReturn(testVehicle);
        when(mockState.getOrderById("ORD001")).thenReturn(testOrder1);
        when(mockState.getOrderById("ORD002")).thenReturn(testOrder2);
        when(mockState.getDepotById("DEP001")).thenReturn(mainDepot);
        when(mockState.getDepotById("DEP002")).thenReturn(auxiliaryDepot);
        when(mockState.getMainDepot()).thenReturn(mainDepot);
        when(mockState.getAuxDepots()).thenReturn(Arrays.asList(auxiliaryDepot));

        // Set up paths for PathFinder static mocking
        startTime = LocalDateTime.now();

        // Initialize static mocking for PathFinder
        mockedPathFinder = mockStatic(PathFinder.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedPathFinder != null) {
            mockedPathFinder.close();
        }
    }

    @Test
    void testCreatePlanFromRoute_withSimpleDelivery() throws Exception {
        // Arrange
        // Create a route with one delivery stop
        RouteStop deliveryStop = new RouteStop(orderPosition1, "ORD001", testOrder1.getDeadlineTime(), 10);
        Route route = new Route("V001", Arrays.asList(deliveryStop), startTime);

        // Mock PathFinder to return a simple path from vehicle to order
        List<Position> pathToOrder = Arrays.asList(
                vehiclePosition,
                new Position(5, 5),
                orderPosition1);

        mockedPathFinder.when(() -> PathFinder.findPath(any(), any(), any(), any()))
                .thenReturn(pathToOrder);

        // Act
        VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, mockState);

        // Assert
        assertNotNull(plan);
        assertEquals(2, plan.getActions().size());

        // First action should be driving to the order
        Action firstAction = plan.getActions().get(0);
        assertEquals(ActionType.DRIVE, firstAction.getType());
        assertTrue(firstAction.getPath().contains(orderPosition1));

        // Second action should be serving the order
        Action secondAction = plan.getActions().get(1);
        assertEquals(ActionType.SERVE, secondAction.getType());
        assertTrue(secondAction.getPath().contains(orderPosition1));
        assertEquals(10, secondAction.getGlpDelivered());
        assertEquals("ORD001", secondAction.getOrderId());
    }
}
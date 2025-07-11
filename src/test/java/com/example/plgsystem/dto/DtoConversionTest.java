package com.example.plgsystem.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.plgsystem.assignation.DeliveryPart;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.RouteStop;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionType;
import com.example.plgsystem.operation.VehiclePlan;

class DtoConversionTest {
    
    @Test
    void testActionDtoConversion() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(30);
        Position start = new Position(0, 0);
        Position end = new Position(5, 5);
        List<Position> path = Arrays.asList(start, new Position(2, 3), end);
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.getId()).thenReturn("ORD001");
        
        Action action = mock(Action.class);
        when(action.getType()).thenReturn(ActionType.DRIVE);
        when(action.getStartTime()).thenReturn(startTime);
        when(action.getEndTime()).thenReturn(endTime);
        when(action.getOrderId()).thenReturn("ORD001");
        when(action.getDepotId()).thenReturn("DEP001");
        when(action.getFuelConsumedGal()).thenReturn(-5.0);
        when(action.getPath()).thenReturn(path);
        
        // Act
        ActionDTO dto = ActionDTO.fromEntity(action);
        
        // Assert
        assertEquals(ActionType.DRIVE, dto.getType());
        assertEquals(startTime, dto.getStartTime());
        assertEquals(endTime, dto.getEndTime());
        assertEquals("ORD001", dto.getOrderId());
        assertEquals(-5.0, dto.getFuelConsumedGal());
        assertEquals(3, dto.getPath().size());
    }
    
    @Test
    void testDeliveryPartDtoConversion() {
        // Arrange
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);
        DeliveryPart deliveryPart = new DeliveryPart("ORD001", 500, deadline);
        
        // Act
        DeliveryPartDTO dto = DeliveryPartDTO.fromEntity(deliveryPart);
        
        // Assert
        assertEquals("ORD001", dto.getOrderId());
        assertEquals(500, dto.getGlpDeliverM3());
        assertEquals(deadline, dto.getDeadlineTime());
    }
    
    @Test
    void testRouteStopDtoConversion() {
        // Arrange
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);
        RouteStop orderStop = new RouteStop(new Position(0, 0), "ORD001", deadline, 500);
        RouteStop depotStop = new RouteStop(new Position(0, 0), "DEP001", 1000);
        
        // Act
        RouteStopDTO orderStopDto = RouteStopDTO.fromEntity(orderStop);
        RouteStopDTO depotStopDto = RouteStopDTO.fromEntity(depotStop);
        
        // Assert
        assertTrue(orderStopDto.isOrderStop());
        assertEquals("ORD001", orderStopDto.getOrderId());
        assertEquals(deadline, orderStopDto.getOrderDeadlineTime());
        assertEquals(500, orderStopDto.getGlpDeliverM3());
        
        assertFalse(depotStopDto.isOrderStop());
        assertEquals("DEP001", depotStopDto.getDepotId());
        assertEquals(1000, depotStopDto.getGlpLoadM3());
    }
    
    @Test
    void testRouteDtoConversion() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime deadline = startTime.plusDays(1);
        RouteStop stop1 = new RouteStop(new Position(0, 0), "DEP001", 1000);
        RouteStop stop2 = new RouteStop(new Position(0, 0), "ORD001", deadline, 500);
        Route route = new Route("V001", Arrays.asList(stop1, stop2), startTime);
        
        // Act
        RouteDTO dto = RouteDTO.fromEntity(route);
        
        // Assert
        assertEquals("V001", dto.getVehicleId());
        assertEquals(startTime, dto.getStartTime());
        assertEquals(2, dto.getStops().size());
        assertEquals("DEP001", dto.getStops().get(0).getDepotId());
        assertEquals("ORD001", dto.getStops().get(1).getOrderId());
    }
    
    @Test
    void testVehiclePlanDtoConversion() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Vehicle mockVehicle = mock(Vehicle.class);
        when(mockVehicle.getId()).thenReturn("V001");
        
        // Create a mock action
        Position depotPos = new Position(5, 5);
        Order mockOrder = mock(Order.class);
        when(mockOrder.getId()).thenReturn("ORD001");
        
        Action action1 = mock(Action.class);
        when(action1.getType()).thenReturn(ActionType.DRIVE);
        when(action1.getStartTime()).thenReturn(now);
        when(action1.getEndTime()).thenReturn(now.plusMinutes(30));
        when(action1.getOrderId()).thenReturn(null);
        when(action1.getDepotId()).thenReturn(null);
        when(action1.getFuelConsumedGal()).thenReturn(-2.0);
        when(action1.getPath()).thenReturn(Arrays.asList(new Position(0, 0), depotPos));
        
        Action action2 = mock(Action.class);
        when(action2.getType()).thenReturn(ActionType.SERVE);
        when(action2.getStartTime()).thenReturn(now.plusMinutes(30));
        when(action2.getEndTime()).thenReturn(now.plusMinutes(45));
        when(action2.getOrderId()).thenReturn("ORD001");
        when(action2.getGlpDelivered()).thenReturn(500);
        when(action2.getDepotId()).thenReturn(null);
        when(action2.getFuelConsumedGal()).thenReturn(-2.0);
        when(action2.getPath()).thenReturn(Arrays.asList(new Position(0, 0), depotPos));
        
        // Create mock VehiclePlan
        VehiclePlan mockPlan = mock(VehiclePlan.class);
        when(mockPlan.getVehicleId()).thenReturn("V001");
        when(mockPlan.getActions()).thenReturn(Arrays.asList(action1, action2));
        when(mockPlan.getStartTime()).thenReturn(now);
        
        // Act
        VehiclePlanDTO dto = VehiclePlanDTO.fromEntity(mockPlan);
        
        // Assert
        assertEquals("V001", dto.getVehicleId());
        assertEquals(now, dto.getStartTime());
        assertEquals(now.plusMinutes(45), dto.getEndTime());
        assertEquals(2, dto.getActions().size());
        assertEquals(null, dto.getActions().get(0).getOrderId());
        assertEquals(0, dto.getActions().get(0).getGlpDelivered());
        assertEquals(now.plusMinutes(30), dto.getActions().get(0).getEndTime());
        assertEquals("ORD001", dto.getActions().get(1).getOrderId());
        assertEquals(500, dto.getActions().get(1).getGlpDelivered());
        assertEquals(now.plusMinutes(45), dto.getActions().get(1).getEndTime());
    }
} 
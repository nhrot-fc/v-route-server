package com.example.plgsystem.operation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;

class ActionFactoryTest {

    @Test
    void testCreateDrivingAction() {
        // Arrange
        List<Position> path = Arrays.asList(
                new Position(0, 0),
                new Position(1, 1),
                new Position(2, 2));
        double fuelConsumedGal = 5.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(1);
        
        // Act
        Action action = ActionFactory.createDrivingAction(path, fuelConsumedGal, startTime, endTime);
        
        // Assert
        assertEquals(ActionType.DRIVE, action.getType());
        assertEquals(path, action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(endTime, action.getEndTime());
        assertEquals(fuelConsumedGal, action.getFuelConsumedGal());
        assertEquals(0, action.getGlpDelivered());
        assertEquals(0, action.getGlpLoaded());
        assertNull(action.getOrderId());
        assertNull(action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
    
    @Test
    void testCreateRefuelingAction() {
        // Arrange
        String depotId = "DEPOT001";
        Position position = new Position(5, 5);
        double refueledGal = 30.0;
        LocalDateTime startTime = LocalDateTime.now();
        
        // Act
        Action action = ActionFactory.createRefuelingAction(depotId, position, startTime, refueledGal);
        
        // Assert
        assertEquals(ActionType.REFUEL, action.getType());
        assertEquals(List.of(position), action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(startTime.plusMinutes(Constants.REFUEL_DURATION_MINUTES), action.getEndTime());
        assertEquals(refueledGal, action.getFuelRefueledGal());
        assertEquals(0, action.getGlpDelivered());
        assertEquals(0, action.getGlpLoaded());
        assertNull(action.getOrderId());
        assertEquals(depotId, action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
    
    @Test
    void testCreateRefillingAction() {
        // Arrange
        String depotId = "DEPOT002";
        Position position = new Position(5, 5);
        int glpAmountAdded = 1000;
        LocalDateTime startTime = LocalDateTime.now();
        
        // Act
        Action action = ActionFactory.createRefillingAction(depotId, position, startTime, glpAmountAdded);
        
        // Assert
        assertEquals(ActionType.RELOAD, action.getType());
        assertEquals(List.of(position), action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(startTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES), action.getEndTime());
        assertEquals(0, action.getGlpDelivered());
        assertEquals(glpAmountAdded, action.getGlpLoaded());
        assertEquals(0.0, action.getFuelConsumedGal());
        assertNull(action.getOrderId());
        assertEquals(depotId, action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
    
    @Test
    void testCreateServingAction() {
        // Arrange
        Position position = new Position(3, 3);
        String orderId = "ORDER123";
        int glpDispensedM3 = 500;
        LocalDateTime startTime = LocalDateTime.now();
        
        // Act
        Action action = ActionFactory.createServingAction(position, orderId, glpDispensedM3, startTime);
        
        // Assert
        assertEquals(ActionType.SERVE, action.getType());
        assertEquals(List.of(position), action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(startTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES), action.getEndTime());
        assertEquals(glpDispensedM3, action.getGlpDelivered());
        assertEquals(0, action.getGlpLoaded());
        assertEquals(0.0, action.getFuelConsumedGal());
        assertEquals(orderId, action.getOrderId());
        assertNull(action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
    
    @Test
    void testCreateIdleAction() {
        // Arrange
        Position position = new Position(1, 1);
        Duration duration = Duration.ofMinutes(30);
        LocalDateTime startTime = LocalDateTime.now();
        
        // Act
        Action action = ActionFactory.createIdleAction(position, duration, startTime);
        
        // Assert
        assertEquals(ActionType.WAIT, action.getType());
        assertEquals(List.of(position), action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(startTime.plus(duration), action.getEndTime());
        assertEquals(0, action.getGlpDelivered());
        assertEquals(0, action.getGlpLoaded());
        assertEquals(0.0, action.getFuelConsumedGal());
        assertNull(action.getOrderId());
        assertNull(action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
    
    @Test
    void testCreateMaintenanceAction() {
        // Arrange
        Position position = new Position(2, 2);
        Duration duration = Duration.ofHours(2);
        LocalDateTime startTime = LocalDateTime.now();
        
        // Act
        Action action = ActionFactory.createMaintenanceAction(position, duration, startTime);
        
        // Assert
        assertEquals(ActionType.MAINTENANCE, action.getType());
        assertEquals(List.of(position), action.getPath());
        assertEquals(startTime, action.getStartTime());
        assertEquals(startTime.plus(duration), action.getEndTime());
        assertEquals(0, action.getGlpDelivered());
        assertEquals(0, action.getGlpLoaded());
        assertEquals(0.0, action.getFuelConsumedGal());
        assertNull(action.getOrderId());
        assertNull(action.getDepotId());
        assertEquals(0.0, action.getCurrentProgress());
    }
}
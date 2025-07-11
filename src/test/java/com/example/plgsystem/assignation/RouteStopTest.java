package com.example.plgsystem.assignation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import com.example.plgsystem.model.Position;

class RouteStopTest {

    @Test
    void testOrderStopCreation() {
        // Arrange
        String orderId = "ORD-123";
        LocalDateTime deadlineTime = LocalDateTime.now().plusHours(2);
        int glpDeliverM3 = 5;
        Position position = new Position(10, 20);
        
        // Act
        RouteStop stop = new RouteStop(position, orderId, deadlineTime, glpDeliverM3);
        
        // Assert
        assertTrue(stop.isOrderStop());
        assertEquals(position, stop.getPosition());
        assertEquals(orderId, stop.getOrderId());
        assertEquals(deadlineTime, stop.getOrderDeadlineTime());
        assertEquals(glpDeliverM3, stop.getGlpDeliverM3());
        assertEquals(0, stop.getGlpLoadM3());
        assertNull(stop.getDepotId());
    }
    
    @Test
    void testDepotStopCreation() {
        // Arrange
        String depotId = "DEP-001";
        int glpLoadM3 = 10;
        Position position = new Position(15, 25);
        
        // Act
        RouteStop stop = new RouteStop(position, depotId, glpLoadM3);
        
        // Assert
        assertFalse(stop.isOrderStop());
        assertEquals(position, stop.getPosition());
        assertEquals(depotId, stop.getDepotId());
        assertEquals(glpLoadM3, stop.getGlpLoadM3());
        assertEquals(0, stop.getGlpDeliverM3());
        assertNull(stop.getOrderId());
        assertNull(stop.getOrderDeadlineTime());
    }
} 
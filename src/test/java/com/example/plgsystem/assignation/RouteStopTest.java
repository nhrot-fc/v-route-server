package com.example.plgsystem.assignation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class RouteStopTest {

    @Test
    void testOrderStopCreation() {
        // Arrange
        String orderId = "ORD-123";
        LocalDateTime deadlineTime = LocalDateTime.now().plusHours(2);
        int glpDeliverM3 = 5;
        
        // Act
        RouteStop stop = new RouteStop(orderId, deadlineTime, glpDeliverM3);
        
        // Assert
        assertTrue(stop.isOrderStop());
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
        
        // Act
        RouteStop stop = new RouteStop(depotId, glpLoadM3);
        
        // Assert
        assertFalse(stop.isOrderStop());
        assertEquals(depotId, stop.getDepotId());
        assertEquals(glpLoadM3, stop.getGlpLoadM3());
        assertEquals(0, stop.getGlpDeliverM3());
        assertNull(stop.getOrderId());
        assertNull(stop.getOrderDeadlineTime());
    }
    
    @Test
    void testLegacyConstructor() {
        // Arrange
        boolean isOrderStop = true;
        String orderId = "ORD-456";
        String depotId = null;
        int glpDeliverM3 = 7;
        int glpLoadM3 = 0;
        
        // Act
        RouteStop stop = new RouteStop(isOrderStop, orderId, null, glpDeliverM3, glpLoadM3);
        
        // Assert
        assertTrue(stop.isOrderStop());
        assertEquals(orderId, stop.getOrderId());
        assertNull(stop.getDepotId());
        assertEquals(glpDeliverM3, stop.getGlpDeliverM3());
        assertEquals(glpLoadM3, stop.getGlpLoadM3());
        assertNull(stop.getOrderDeadlineTime());
    }
} 
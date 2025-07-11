package com.example.plgsystem.assignation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.example.plgsystem.model.Position;

class RouteTest {

    @Test
    void testRouteCreation() {
        // Arrange
        String vehicleId = "V-001";
        LocalDateTime startTime = LocalDateTime.now();
        
        RouteStop stop1 = new RouteStop(new Position(0, 0), "ORD-1", LocalDateTime.now().plusHours(2), 5);
        RouteStop stop2 = new RouteStop(new Position(0, 0), "ORD-2", LocalDateTime.now().plusHours(3), 10);
        List<RouteStop> stops = Arrays.asList(stop1, stop2);
        
        // Act
        Route route = new Route(vehicleId, stops, startTime);
        
        // Assert
        assertEquals(vehicleId, route.vehicleId());
        assertEquals(stops, route.stops());
        assertEquals(startTime, route.startTime());
        assertEquals(2, route.stops().size());
    }
} 
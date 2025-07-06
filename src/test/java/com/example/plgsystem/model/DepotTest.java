package com.example.plgsystem.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DepotTest {

    @Test
    public void testDepotCreation() {
        // Given
        Position position = new Position(10, 20);
        String depotId = "D001";
        int capacity = 1000;
        boolean canRefuel = true;
        
        // When
        Depot depot = new Depot(depotId, position, capacity, canRefuel);
        
        // Then
        assertEquals(depotId, depot.getId());
        assertEquals(position, depot.getPosition());
        assertEquals(capacity, depot.getGlpCapacityM3());
        assertEquals(canRefuel, depot.isCanRefuel());
        assertEquals(0, depot.getCurrentGlpM3()); // Initial value should be zero
    }
    
    @Test
    public void testDepotRefill() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        
        // When
        depot.refillGLP();
        
        // Then
        assertEquals(1000, depot.getCurrentGlpM3());
    }
    
    @Test
    public void testDepotServeGLP() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        depot.refillGLP(); // Fill to capacity
        
        // When
        depot.serveGLP(200);
        
        // Then
        assertEquals(800, depot.getCurrentGlpM3());
        
        // Test over-serving (shouldn't go below zero)
        depot.serveGLP(1000);
        assertEquals(0, depot.getCurrentGlpM3());
    }
    
    @Test
    public void testCanServeGLP() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        depot.setCurrentGlpM3(500);
        
        // Then
        assertTrue(depot.canServeGLP(300));
        assertTrue(depot.canServeGLP(500));
        assertFalse(depot.canServeGLP(600));
    }
    
    @Test
    public void testSetCurrentGlpM3() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        
        // When - set to normal value
        depot.setCurrentGlpM3(500);
        
        // Then
        assertEquals(500, depot.getCurrentGlpM3());
        
        // When - set to negative (should be capped at zero)
        depot.setCurrentGlpM3(-100);
        
        // Then
        assertEquals(0, depot.getCurrentGlpM3());
        
        // When - set above capacity (should be capped at capacity)
        depot.setCurrentGlpM3(1200);
        
        // Then
        assertEquals(1000, depot.getCurrentGlpM3());
    }
}

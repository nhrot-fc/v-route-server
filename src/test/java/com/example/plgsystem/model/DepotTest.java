package com.example.plgsystem.model;

import com.example.plgsystem.enums.DepotType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DepotTest {

    @Test
    public void testDepotCreation() {
        // Given
        Position position = new Position(10, 20);
        String depotId = "D001";
        int capacity = 1000;
        DepotType type = DepotType.AUXILIARY;
        
        // When
        Depot depot = new Depot(depotId, position, capacity, type);
        
        // Then
        assertEquals(depotId, depot.getId());
        assertEquals(position, depot.getPosition());
        assertEquals(capacity, depot.getGlpCapacityM3());
        assertEquals(type, depot.getType());
        assertEquals(capacity, depot.getCurrentGlpM3()); // Initial value should be zero
    }
    
    @Test
    public void testDepotRefill() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, DepotType.AUXILIARY);
        
        // When
        depot.refill();
        
        // Then
        assertEquals(1000, depot.getCurrentGlpM3());
    }
    
    @Test
    public void testDepotServeGlp() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, DepotType.AUXILIARY);
        depot.refill(); // Fill to capacity
        
        // When
        depot.serve(200);
        
        // Then
        assertEquals(800, depot.getCurrentGlpM3());
        
        // Test over-serving (shouldn't go below zero)
        depot.serve(1000);
        assertEquals(0, depot.getCurrentGlpM3());
    }
    
    @Test
    public void testMainDepotServe() {
        // Given
        Depot mainDepot = new Depot("MAIN", new Position(10, 20), 5000, DepotType.MAIN);
        mainDepot.refill();
        int initialGlp = mainDepot.getCurrentGlpM3();
        
        // When
        mainDepot.serve(1000);
        
        // Then - main depot's GLP should not be reduced
        assertEquals(initialGlp, mainDepot.getCurrentGlpM3());
    }
    
    @Test
    public void testCanServe() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, DepotType.AUXILIARY);
        depot.refill();
        depot.serve(500); // Now has 500 GLP
        
        // Then
        assertTrue(depot.canServe(300));
        assertTrue(depot.canServe(500));
        assertFalse(depot.canServe(600));
    }
    
    @Test
    public void testIsMainAndIsAuxiliary() {
        // Given
        Depot mainDepot = new Depot("MAIN", new Position(10, 20), 5000, DepotType.MAIN);
        Depot auxDepot = new Depot("AUX1", new Position(30, 40), 1000, DepotType.AUXILIARY);
        
        // Then
        assertTrue(mainDepot.isMain());
        assertFalse(mainDepot.isAuxiliary());
        
        assertFalse(auxDepot.isMain());
        assertTrue(auxDepot.isAuxiliary());
    }
}

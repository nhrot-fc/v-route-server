package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VehicleTest {

    @Test
    public void testVehicleCreation() {
        // Given
        Position position = new Position(10, 20);
        
        // When
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();
        
        // Then
        assertEquals("V001", vehicle.getId());
        assertEquals(VehicleType.TA, vehicle.getType());
        assertEquals(VehicleType.TA.getCapacityM3(), vehicle.getGlpCapacityM3());
        assertEquals(position, vehicle.getCurrentPosition());
        assertEquals(0, vehicle.getCurrentGlpM3());
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
    }

    @Test
    public void testUpdateVehicleProperties() {
        // Given
        Position initialPosition = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(initialPosition)
                .build();
        
        // When
        Position newPosition = new Position(15, 25);
        vehicle.setCurrentPosition(newPosition);
        vehicle.setStatus(VehicleStatus.IN_ROUTE);
        
        // Then
        assertEquals(newPosition, vehicle.getCurrentPosition());
        assertEquals(VehicleStatus.IN_ROUTE, vehicle.getStatus());
    }

    @Test
    public void testVehicleFuelConsumption() {
        // Given
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        
        double initialFuel = vehicle.getCurrentFuelGal();
        
        // When
        vehicle.consumeFuel(100.0); // 100km
        
        // Then
        assertTrue(vehicle.getCurrentFuelGal() < initialFuel);
    }

    @Test
    public void testVehicleRefill() {
        // Given
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        
        // Initial state - empty GLP
        assertEquals(0, vehicle.getCurrentGlpM3());
        
        // When
        vehicle.refill(5);
        
        // Then
        assertEquals(5, vehicle.getCurrentGlpM3());
        
        // When - attempt to overfill
        vehicle.refill(VehicleType.TA.getCapacityM3() + 10);
        
        // Then - should not exceed capacity
        assertEquals(VehicleType.TA.getCapacityM3(), vehicle.getCurrentGlpM3());
    }

    @Test
    public void testVehicleGlpDispensing() {
        // Given
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
                
        // Fill the tank first
        vehicle.refill(10);
        assertEquals(10, vehicle.getCurrentGlpM3());
        
        // When
        vehicle.dispenseGlp(3);
        
        // Then
        assertEquals(7, vehicle.getCurrentGlpM3());
        
        // Test canDispenseGLP
        assertTrue(vehicle.canDispenseGLP(5));
        assertFalse(vehicle.canDispenseGLP(10));
    }
    
    @Test
    public void testVehicleRefuel() {
        // Given
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
                
        // Consume some fuel
        vehicle.consumeFuel(100.0);
        double reducedFuel = vehicle.getCurrentFuelGal();
        
        // When
        vehicle.refuel();
        
        // Then
        assertEquals(vehicle.getFuelCapacityGal(), vehicle.getCurrentFuelGal());
        assertTrue(vehicle.getCurrentFuelGal() > reducedFuel);
    }
}

package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

    @Test
    public void testSimulationCreation() {
        // Given
        SimulationState state = createSampleSimulationState();
        
        // When
        Simulation simulation = new Simulation(state);
        
        // Then
        assertNotNull(simulation.getId());
        assertEquals(state, simulation.getState());
        assertNotNull(simulation.getCreatedAt());
        assertEquals(simulation.getCreatedAt(), simulation.getLastUpdated());
    }

    @Test
    public void testSimulationWithCustomFields() {
        // Given
        SimulationState state = createSampleSimulationState();
        String name = "Test Simulation";
        String description = "A test simulation for unit testing";
        
        // When
        Simulation simulation = new Simulation(state, name, description);
        
        // Then
        assertEquals(name, simulation.getName());
        assertEquals(description, simulation.getDescription());
    }

    @Test
    public void testUpdateSimulationState() {
        // Given
        SimulationState originalState = createSampleSimulationState();
        Simulation simulation = new Simulation(originalState);
        LocalDateTime originalLastUpdated = simulation.getLastUpdated();
        
        // Pause to ensure timestamp will be different
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // When
        SimulationState newState = createAnotherSimulationState();
        simulation.setState(newState);
        
        // Then
        assertEquals(newState, simulation.getState());
        assertTrue(simulation.getLastUpdated().isAfter(originalLastUpdated));
    }

    @Test
    public void testUpdateNameAndDescription() {
        // Given
        Simulation simulation = new Simulation(createSampleSimulationState());
        String originalName = simulation.getName();
        String originalDescription = simulation.getDescription();
        LocalDateTime originalLastUpdated = simulation.getLastUpdated();
        
        // Pause to ensure timestamp will be different
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // When
        String newName = "Updated Name";
        String newDescription = "Updated Description";
        simulation.setName(newName);
        simulation.setDescription(newDescription);
        
        // Then
        assertNotEquals(originalName, simulation.getName());
        assertNotEquals(originalDescription, simulation.getDescription());
        assertEquals(newName, simulation.getName());
        assertEquals(newDescription, simulation.getDescription());
        assertTrue(simulation.getLastUpdated().isAfter(originalLastUpdated));
    }

    @Test
    public void testAdvanceTime() {
        // Given
        LocalDateTime initialTime = LocalDateTime.of(2025, 7, 5, 10, 0);
        SimulationState state = createSampleSimulationState(initialTime);
        Simulation simulation = new Simulation(state);
        
        // When
        simulation.advanceTime(30); // advance 30 minutes
        
        // Then
        LocalDateTime expectedTime = initialTime.plusMinutes(30);
        assertEquals(expectedTime, state.getCurrentTime());
    }

    private SimulationState createSampleSimulationState() {
        return createSampleSimulationState(LocalDateTime.now());
    }
    
    private SimulationState createSampleSimulationState(LocalDateTime time) {
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        vehicles.add(vehicle);
        
        Depot mainDepot = new Depot("MD001", new Position(0, 0), 10000, true);
        
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AD001", new Position(50, 50), 2000, false));
        
        return new SimulationState(vehicles, mainDepot, auxDepots, time);
    }
    
    private SimulationState createAnotherSimulationState() {
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(15, 25))
                .build();
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TB)
                .currentPosition(new Position(30, 40))
                .build();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);
        
        Depot mainDepot = new Depot("MD001", new Position(0, 0), 10000, true);
        
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AD001", new Position(50, 50), 2000, false));
        auxDepots.add(new Depot("AD002", new Position(70, 70), 1500, true));
        
        return new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
    }
}

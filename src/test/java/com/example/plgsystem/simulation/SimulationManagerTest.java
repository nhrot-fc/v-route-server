package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationManagerTest {

    @Test
    public void testCreateAndGetSimulation() {
        // Given
        SimulationState state = createSampleSimulationState();
        
        // When
        Simulation simulation = SimulationManager.createSimulation(state);
        Optional<Simulation> retrievedSimulation = SimulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(retrievedSimulation.isPresent());
        assertEquals(simulation.getId(), retrievedSimulation.get().getId());
    }

    @Test
    public void testCreateNamedSimulation() {
        // Given
        SimulationState state = createSampleSimulationState();
        String name = "Test Simulation";
        String description = "A test simulation for unit testing";
        
        // When
        Simulation simulation = SimulationManager.createSimulation(state, name, description);
        Optional<Simulation> retrievedSimulation = SimulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(retrievedSimulation.isPresent());
        assertEquals(name, retrievedSimulation.get().getName());
        assertEquals(description, retrievedSimulation.get().getDescription());
    }

    @Test
    public void testGetAllSimulations() {
        // Given - clear existing simulations
        SimulationManager.clearAllSimulations();
        
        // When
        SimulationState state1 = createSampleSimulationState();
        SimulationState state2 = createAnotherSimulationState();
        
        SimulationManager.createSimulation(state1, "Sim 1", "First simulation");
        SimulationManager.createSimulation(state2, "Sim 2", "Second simulation");
        
        List<Simulation> allSimulations = SimulationManager.getAllSimulations();
        
        // Then
        assertEquals(2, allSimulations.size());
    }

    @Test
    public void testUpdateSimulation() {
        // Given
        SimulationState state = createSampleSimulationState();
        Simulation simulation = SimulationManager.createSimulation(state);
        
        // When
        String newName = "Updated Name";
        simulation.setName(newName);
        boolean updateResult = SimulationManager.updateSimulation(simulation);
        Optional<Simulation> retrievedSimulation = SimulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(updateResult);
        assertTrue(retrievedSimulation.isPresent());
        assertEquals(newName, retrievedSimulation.get().getName());
    }

    @Test
    public void testDeleteSimulation() {
        // Given
        SimulationState state = createSampleSimulationState();
        Simulation simulation = SimulationManager.createSimulation(state);
        
        // When
        boolean deleteResult = SimulationManager.deleteSimulation(simulation.getId());
        Optional<Simulation> retrievedSimulation = SimulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(deleteResult);
        assertFalse(retrievedSimulation.isPresent());
    }

    @Test
    public void testGetSimulationCount() {
        // Given - clear existing simulations
        SimulationManager.clearAllSimulations();
        
        // Initial count should be 0
        assertEquals(0, SimulationManager.getSimulationCount());
        
        // When - add some simulations
        SimulationState state1 = createSampleSimulationState();
        SimulationState state2 = createAnotherSimulationState();
        
        SimulationManager.createSimulation(state1);
        SimulationManager.createSimulation(state2);
        
        // Then
        assertEquals(2, SimulationManager.getSimulationCount());
        
        // When - clear all
        SimulationManager.clearAllSimulations();
        
        // Then
        assertEquals(0, SimulationManager.getSimulationCount());
    }

    private SimulationState createSampleSimulationState() {
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
        
        return new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
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

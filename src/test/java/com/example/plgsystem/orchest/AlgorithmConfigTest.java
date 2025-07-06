package com.example.plgsystem.orchest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AlgorithmConfigTest {

    @Test
    public void testCreateDefault() {
        AlgorithmConfig config = AlgorithmConfig.createDefault();
        
        assertEquals(AlgorithmConfig.SimulationMode.SIMULATION_WEEKLY, config.getMode());
        assertEquals(60, config.getSimulationStepSeconds());
        assertEquals(1, config.getSimulationStepMinutes());
        assertEquals(7, config.getSimulationMaxDays());
        assertFalse(config.isUseDatabase());
        assertEquals(24, config.getDataLoadHorizonHours());
    }

    @Test
    public void testCreateOperationsMode() {
        AlgorithmConfig config = AlgorithmConfig.createOperationsMode();
        
        assertEquals(AlgorithmConfig.SimulationMode.OPERATIONS_DAILY, config.getMode());
        assertEquals(1, config.getSimulationStepSeconds());
        assertTrue(config.isUseDatabase());
        assertEquals(24, config.getDataLoadHorizonHours());
    }

    @Test
    public void testCreateWeeklySimulation() {
        AlgorithmConfig config = AlgorithmConfig.createWeeklySimulation();
        
        assertEquals(AlgorithmConfig.SimulationMode.SIMULATION_WEEKLY, config.getMode());
        assertEquals(60, config.getSimulationStepSeconds());
        assertEquals(1, config.getSimulationStepMinutes());
        assertEquals(7, config.getSimulationMaxDays());
        assertFalse(config.isUseDatabase());
        assertEquals(168, config.getDataLoadHorizonHours());
    }

    @Test
    public void testCreateCollapseSimulation() {
        AlgorithmConfig config = AlgorithmConfig.createCollapseSimulation();
        
        assertEquals(AlgorithmConfig.SimulationMode.SIMULATION_COLLAPSE, config.getMode());
        assertEquals(60, config.getSimulationStepSeconds());
        assertEquals(1, config.getSimulationStepMinutes());
        assertEquals(Integer.MAX_VALUE, config.getSimulationMaxDays());
        assertFalse(config.isUseDatabase());
    }

    @Test
    public void testSetAndGetMethods() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Test setters
        config.setMode(AlgorithmConfig.SimulationMode.SIMULATION_WEEKLY);
        config.setSimulationStepSeconds(30);
        config.setSimulationMaxDays(14);
        config.setUseDatabase(true);
        config.setDataLoadHorizonHours(48);
        
        // Test getters
        assertEquals(AlgorithmConfig.SimulationMode.SIMULATION_WEEKLY, config.getMode());
        assertEquals(30, config.getSimulationStepSeconds());
        assertEquals(0, config.getSimulationStepMinutes()); // 30 seconds is 0 minutes
        assertEquals(14, config.getSimulationMaxDays());
        assertTrue(config.isUseDatabase());
        assertEquals(48, config.getDataLoadHorizonHours());
    }

    @Test
    public void testSetSimulationStepMinutes() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        config.setSimulationStepMinutes(5);
        assertEquals(300, config.getSimulationStepSeconds()); // 5 minutes = 300 seconds
        assertEquals(5, config.getSimulationStepMinutes());
        
        // Test invalid values
        assertThrows(IllegalArgumentException.class, () -> config.setSimulationStepMinutes(0));
        assertThrows(IllegalArgumentException.class, () -> config.setSimulationStepMinutes(-1));
    }

    @Test
    public void testSetSimulationMaxDays() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        config.setSimulationMaxDays(30);
        assertEquals(30, config.getSimulationMaxDays());
        
        // Test invalid values
        assertThrows(IllegalArgumentException.class, () -> config.setSimulationMaxDays(0));
        assertThrows(IllegalArgumentException.class, () -> config.setSimulationMaxDays(-1));
    }
} 
package com.example.plgsystem.orchest;

/**
 * Configuration for simulation and algorithm parameters.
 * This class contains parameters that control various aspects of the simulation
 * and metaheuristic algorithms used for optimization.
 */
public class AlgorithmConfig {
    
    // Simulation mode
    public enum SimulationMode {
        OPERATIONS_DAILY,    // Real-time operations mode with 1-second ticks
        SIMULATION_WEEKLY,   // Weekly simulation with 1-minute ticks
        SIMULATION_COLLAPSE  // Collapse simulation with 1-minute ticks
    }
    
    private SimulationMode mode;
    
    // Simulation parameters
    private int simulationStepSeconds;    // How many seconds to advance per simulation tick
    private int simulationMaxDays;        // Maximum number of days to simulate
    private boolean useDatabase;          // Whether to use database for data
    private int dataLoadHorizonHours;     // How many hours of data to load at once (for operational mode)
    
    // Metaheuristic parameters
    private int tabuSize;                 // Size of tabu list for metaheuristic
    private int maxIterations;            // Maximum number of iterations for optimization
    private int maxTimeSeconds;           // Maximum time in seconds for optimization
    private double temperatureStart;      // Starting temperature for simulated annealing
    private double temperatureDecay;      // Temperature decay rate
    
    // Vehicle parameters
    private double vehicleSpeed;          // Average vehicle speed (km/hour)
    private double refuelTimeMinutes;     // Minutes needed to refill vehicle at depot
    
    /**
     * Creates a default configuration with reasonable values.
     * 
     * @return Default configuration
     */
    public static AlgorithmConfig createDefault() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Default to weekly simulation mode
        config.mode = SimulationMode.SIMULATION_WEEKLY;
        
        // Simulation parameters
        config.simulationStepSeconds = 60;  // Default to 1 minute per tick
        config.simulationMaxDays = 7;       // Simulate up to 7 days
        config.useDatabase = false;         // Don't use database by default
        config.dataLoadHorizonHours = 24;   // Load 24 hours of data at once
        
        // Metaheuristic parameters
        config.tabuSize = 10;
        config.maxIterations = 1000;
        config.maxTimeSeconds = 5;          // 5 seconds max per optimization
        config.temperatureStart = 100.0;
        config.temperatureDecay = 0.95;
        
        // Vehicle parameters
        config.vehicleSpeed = 30.0;         // 30 km/h average speed
        config.refuelTimeMinutes = 30;      // 30 minutes to refill
        
        return config;
    }

    /**
     * Creates a configuration for daily operations mode.
     * 
     * @return Operations mode configuration with 1-second ticks and database integration
     */
    public static AlgorithmConfig createOperationsMode() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Operations daily mode
        config.mode = SimulationMode.OPERATIONS_DAILY;
        
        // Simulation parameters
        config.simulationStepSeconds = 1;   // Advance 1 second per tick for real-time operation
        config.simulationMaxDays = 365;     // Long-running operations
        config.useDatabase = true;          // Use database for data
        config.dataLoadHorizonHours = 24;   // Load 24 hours of data at once
        
        // Metaheuristic parameters - optimized for real-time
        config.tabuSize = 7;
        config.maxIterations = 500;
        config.maxTimeSeconds = 2;          // Fast optimization for real-time
        config.temperatureStart = 80.0;
        config.temperatureDecay = 0.9;
        
        // Vehicle parameters - same as default
        config.vehicleSpeed = 30.0;
        config.refuelTimeMinutes = 30;
        
        return config;
    }

    /**
     * Creates a configuration for weekly simulation mode.
     * 
     * @return Weekly simulation configuration with 1-minute ticks
     */
    public static AlgorithmConfig createWeeklySimulation() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Weekly simulation mode
        config.mode = SimulationMode.SIMULATION_WEEKLY;
        
        // Simulation parameters
        config.simulationStepSeconds = 60;  // Advance 1 minute per tick
        config.simulationMaxDays = 7;       // Exactly 7 days for weekly simulation
        config.useDatabase = false;         // Use files instead of database
        config.dataLoadHorizonHours = 168;  // Load all data (7 days * 24 hours)
        
        // Metaheuristic parameters
        config.tabuSize = 10;
        config.maxIterations = 1000;
        config.maxTimeSeconds = 5;
        config.temperatureStart = 100.0;
        config.temperatureDecay = 0.95;
        
        // Vehicle parameters
        config.vehicleSpeed = 30.0;
        config.refuelTimeMinutes = 30;
        
        return config;
    }

    /**
     * Creates a configuration for collapse simulation (stress test) mode.
     * 
     * @return Collapse simulation configuration with 1-minute ticks and no time limit
     */
    public static AlgorithmConfig createCollapseSimulation() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Collapse simulation mode
        config.mode = SimulationMode.SIMULATION_COLLAPSE;
        
        // Simulation parameters
        config.simulationStepSeconds = 60;    // Advance 1 minute per tick (same as weekly)
        config.simulationMaxDays = Integer.MAX_VALUE;  // No time limit until collapse
        config.useDatabase = false;           // Use files instead of database
        config.dataLoadHorizonHours = 1000;   // Load a large amount of data
        
        // Metaheuristic parameters
        config.tabuSize = 10;
        config.maxIterations = 1000;
        config.maxTimeSeconds = 5;
        config.temperatureStart = 100.0;
        config.temperatureDecay = 0.95;
        
        // Vehicle parameters
        config.vehicleSpeed = 30.0;
        config.refuelTimeMinutes = 30;
        
        return config;
    }
    
    // Getters and setters
    
    public SimulationMode getMode() {
        return mode;
    }
    
    public void setMode(SimulationMode mode) {
        this.mode = mode;
    }
    
    public int getSimulationStepSeconds() {
        return simulationStepSeconds;
    }
    
    public void setSimulationStepSeconds(int simulationStepSeconds) {
        if (simulationStepSeconds <= 0) {
            throw new IllegalArgumentException("Simulation step must be positive");
        }
        this.simulationStepSeconds = simulationStepSeconds;
    }
    
    public int getSimulationStepMinutes() {
        return simulationStepSeconds / 60;
    }

    public void setSimulationStepMinutes(int simulationStepMinutes) {
        if (simulationStepMinutes <= 0) {
            throw new IllegalArgumentException("Simulation step must be positive");
        }
        this.simulationStepSeconds = simulationStepMinutes * 60;
    }

    public int getSimulationMaxDays() {
        return simulationMaxDays;
    }

    public void setSimulationMaxDays(int simulationMaxDays) {
        if (simulationMaxDays <= 0) {
            throw new IllegalArgumentException("Maximum simulation days must be positive");
        }
        this.simulationMaxDays = simulationMaxDays;
    }
    
    public boolean isUseDatabase() {
        return useDatabase;
    }
    
    public void setUseDatabase(boolean useDatabase) {
        this.useDatabase = useDatabase;
    }
    
    public int getDataLoadHorizonHours() {
        return dataLoadHorizonHours;
    }
    
    public void setDataLoadHorizonHours(int dataLoadHorizonHours) {
        this.dataLoadHorizonHours = dataLoadHorizonHours;
    }

    public int getTabuSize() {
        return tabuSize;
    }

    public void setTabuSize(int tabuSize) {
        this.tabuSize = tabuSize;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxTimeSeconds() {
        return maxTimeSeconds;
    }

    public void setMaxTimeSeconds(int maxTimeSeconds) {
        this.maxTimeSeconds = maxTimeSeconds;
    }

    public double getTemperatureStart() {
        return temperatureStart;
    }

    public void setTemperatureStart(double temperatureStart) {
        this.temperatureStart = temperatureStart;
    }

    public double getTemperatureDecay() {
        return temperatureDecay;
    }

    public void setTemperatureDecay(double temperatureDecay) {
        this.temperatureDecay = temperatureDecay;
    }

    public double getVehicleSpeed() {
        return vehicleSpeed;
    }

    public void setVehicleSpeed(double vehicleSpeed) {
        this.vehicleSpeed = vehicleSpeed;
    }

    public double getRefuelTimeMinutes() {
        return refuelTimeMinutes;
    }

    public void setRefuelTimeMinutes(double refuelTimeMinutes) {
        this.refuelTimeMinutes = refuelTimeMinutes;
    }
}

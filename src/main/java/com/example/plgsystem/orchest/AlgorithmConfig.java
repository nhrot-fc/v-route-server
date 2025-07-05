package com.example.plgsystem.orchest;

/**
 * Configuration for simulation and algorithm parameters.
 * This class contains parameters that control various aspects of the simulation
 * and metaheuristic algorithms used for optimization.
 */
public class AlgorithmConfig {
    
    // Simulation parameters
    private int simulationStepMinutes;     // How many minutes to advance per simulation tick
    private int simulationMaxDays;         // Maximum number of days to simulate
    
    // Metaheuristic parameters
    private int tabuSize;                  // Size of tabu list for metaheuristic
    private int maxIterations;             // Maximum number of iterations for optimization
    private int maxTimeSeconds;            // Maximum time in seconds for optimization
    private double temperatureStart;       // Starting temperature for simulated annealing
    private double temperatureDecay;       // Temperature decay rate
    
    // Vehicle parameters
    private double vehicleSpeed;           // Average vehicle speed (km/hour)
    private double refuelTimeMinutes;      // Minutes needed to refill vehicle at depot
    
    /**
     * Creates a default configuration with reasonable values.
     * 
     * @return Default configuration
     */
    public static AlgorithmConfig createDefault() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Simulation parameters
        config.simulationStepMinutes = 15;  // Advance 15 minutes per tick
        config.simulationMaxDays = 7;       // Simulate up to 7 days
        
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
     * Creates a configuration optimized for speed.
     * 
     * @return Fast configuration with reduced computational requirements
     */
    public static AlgorithmConfig createFast() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Simulation parameters
        config.simulationStepMinutes = 30;  // Advance 30 minutes per tick
        config.simulationMaxDays = 3;       // Simulate up to 3 days
        
        // Metaheuristic parameters
        config.tabuSize = 5;
        config.maxIterations = 100;
        config.maxTimeSeconds = 1;          // 1 second max per optimization
        config.temperatureStart = 50.0;
        config.temperatureDecay = 0.9;
        
        // Vehicle parameters - same as default
        config.vehicleSpeed = 30.0;
        config.refuelTimeMinutes = 30;
        
        return config;
    }

    /**
     * Creates a configuration optimized for quality.
     * 
     * @return High quality configuration with increased computational requirements
     */
    public static AlgorithmConfig createQuality() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Simulation parameters
        config.simulationStepMinutes = 5;   // Advance 5 minutes per tick (more precision)
        config.simulationMaxDays = 10;      // Simulate up to 10 days
        
        // Metaheuristic parameters
        config.tabuSize = 20;
        config.maxIterations = 5000;
        config.maxTimeSeconds = 30;         // 30 seconds max per optimization
        config.temperatureStart = 200.0;
        config.temperatureDecay = 0.98;
        
        // Vehicle parameters - same as default
        config.vehicleSpeed = 30.0;
        config.refuelTimeMinutes = 30;
        
        return config;
    }
    
    // Getters and setters
    
    public int getSimulationStepMinutes() {
        return simulationStepMinutes;
    }

    public void setSimulationStepMinutes(int simulationStepMinutes) {
        if (simulationStepMinutes <= 0) {
            throw new IllegalArgumentException("Simulation step must be positive");
        }
        this.simulationStepMinutes = simulationStepMinutes;
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

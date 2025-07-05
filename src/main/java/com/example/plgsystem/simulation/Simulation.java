package com.example.plgsystem.simulation;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Represents a simulation instance that contains a unique identifier and the current state
 * of the simulation. This class is used to manage simulations in memory without database
 * interaction, primarily for visualization purposes in the map interface.
 */
public class Simulation {
    private final String id;
    private SimulationState state;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String name;
    private String description;

    /**
     * Creates a new simulation with the provided simulation state and a randomly generated ID.
     * 
     * @param state The initial simulation state
     */
    public Simulation(SimulationState state) {
        this.id = UUID.randomUUID().toString();
        this.state = state;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
        this.name = "Simulation " + this.createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.description = "Simulation created at " + this.createdAt;
    }

    /**
     * Creates a new simulation with the provided simulation state, name, description, and a randomly generated ID.
     * 
     * @param state The initial simulation state
     * @param name The name of the simulation
     * @param description A brief description of the simulation
     */
    public Simulation(SimulationState state, String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.state = state;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the unique identifier of the simulation.
     * 
     * @return The simulation ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the current state of the simulation.
     * 
     * @return The simulation state
     */
    public SimulationState getState() {
        return state;
    }

    /**
     * Updates the simulation state.
     * 
     * @param state The new simulation state
     */
    public void setState(SimulationState state) {
        this.state = state;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the creation timestamp of the simulation.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last update timestamp of the simulation.
     * 
     * @return The last update timestamp
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Gets the name of the simulation.
     * 
     * @return The simulation name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the simulation.
     * 
     * @param name The new simulation name
     */
    public void setName(String name) {
        this.name = name;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Gets the description of the simulation.
     * 
     * @return The simulation description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the simulation.
     * 
     * @param description The new simulation description
     */
    public void setDescription(String description) {
        this.description = description;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Advances the simulation time by the specified number of minutes.
     * 
     * @param minutes The number of minutes to advance the simulation
     */
    public void advanceTime(int minutes) {
        this.state.advanceTime(minutes);
        this.lastUpdated = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Simulation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                ", state=" + state +
                '}';
    }
}

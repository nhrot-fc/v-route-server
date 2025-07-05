package com.example.plgsystem.simulation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Manages multiple simulation instances in memory.
 * This class provides methods to create, retrieve, and delete simulations.
 * All data is stored in memory, with no database interactions.
 */
public class SimulationManager {
    private static final Map<String, Simulation> simulations = new ConcurrentHashMap<>();

    /**
     * Creates a new simulation and stores it in memory.
     *
     * @param state The initial simulation state
     * @return The created simulation
     */
    public static Simulation createSimulation(SimulationState state) {
        Simulation simulation = new Simulation(state);
        simulations.put(simulation.getId(), simulation);
        return simulation;
    }

    /**
     * Creates a new simulation with a name and description and stores it in memory.
     *
     * @param state The initial simulation state
     * @param name The name of the simulation
     * @param description A brief description of the simulation
     * @return The created simulation
     */
    public static Simulation createSimulation(SimulationState state, String name, String description) {
        Simulation simulation = new Simulation(state, name, description);
        simulations.put(simulation.getId(), simulation);
        return simulation;
    }

    /**
     * Gets a simulation by its ID.
     *
     * @param id The ID of the simulation to retrieve
     * @return An Optional containing the simulation if found, or empty if not found
     */
    public static Optional<Simulation> getSimulation(String id) {
        return Optional.ofNullable(simulations.get(id));
    }

    /**
     * Gets all simulations.
     *
     * @return A list of all simulations
     */
    public static List<Simulation> getAllSimulations() {
        return new ArrayList<>(simulations.values());
    }

    /**
     * Updates an existing simulation.
     *
     * @param simulation The simulation to update
     * @return true if the update was successful, false if the simulation does not exist
     */
    public static boolean updateSimulation(Simulation simulation) {
        if (simulations.containsKey(simulation.getId())) {
            simulations.put(simulation.getId(), simulation);
            return true;
        }
        return false;
    }

    /**
     * Deletes a simulation by its ID.
     *
     * @param id The ID of the simulation to delete
     * @return true if the simulation was deleted, false if it did not exist
     */
    public static boolean deleteSimulation(String id) {
        return simulations.remove(id) != null;
    }

    /**
     * Gets the count of active simulations.
     *
     * @return The number of active simulations
     */
    public static int getSimulationCount() {
        return simulations.size();
    }

    /**
     * Clears all simulations from memory.
     */
    public static void clearAllSimulations() {
        simulations.clear();
    }
}

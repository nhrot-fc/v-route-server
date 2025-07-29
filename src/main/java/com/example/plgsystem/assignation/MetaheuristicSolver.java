package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.simulation.SimulationState;

public class MetaheuristicSolver {
    // Parámetros configurables para el algoritmo
    private static int MAX_ITERATIONS = Constants.MAX_ITERATIONS;
    private static int TABU_TENURE = Constants.TABU_TENURE;
    private static int NUM_NEIGHBORS = Constants.NUM_NEIGHBORS;

    /**
     * Configura los parámetros del algoritmo de búsqueda tabú
     * 
     * @param maxIterations Número máximo de iteraciones
     * @param tabuTenure    Duración de permanencia en la lista tabú
     * @param numNeighbors  Número de vecinos a generar en cada iteración
     */
    public static void configure(int maxIterations, int tabuTenure, int numNeighbors) {
        MetaheuristicSolver.MAX_ITERATIONS = maxIterations;
        MetaheuristicSolver.TABU_TENURE = tabuTenure;
        MetaheuristicSolver.NUM_NEIGHBORS = numNeighbors;
    }

    /**
     * Represents a tabu move in the search space
     */
    private static class TabuMove {
        private final String moveId;
        private int remainingTenure;

        public TabuMove(String moveId, int tenure) {
            this.moveId = moveId;
            this.remainingTenure = tenure;
        }

        public boolean decrementTenure() {
            return --remainingTenure <= 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TabuMove tabuMove = (TabuMove) o;
            return moveId.equals(tabuMove.moveId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moveId);
        }
    }

    /**
     * Generate a set of neighboring solutions by applying different operations to
     * the current solution
     */
    private static List<Map<String, List<DeliveryPart>>> generateNeighbors(
            Map<String, List<DeliveryPart>> currentAssignment,
            SimulationState state,
            int numNeighbors) {
        List<Map<String, List<DeliveryPart>>> neighbors = new ArrayList<>();
        for (int i = 0; i < numNeighbors; i++) {
            neighbors.add(DistributionOperations.randomOperationWithState(currentAssignment, state));
        }
        return neighbors;
    }

    /**
     * Generate a unique identifier for a move between two solutions.
     * This is used to track moves for the tabu list.
     */
    private static String generateMoveId(Map<String, List<DeliveryPart>> fromSolution,
            Map<String, List<DeliveryPart>> toSolution) {
        StringBuilder sb = new StringBuilder();

        // Compare the two solutions and create a digest of the differences
        for (String vehicleId : fromSolution.keySet()) {
            List<DeliveryPart> fromDeliveries = fromSolution.get(vehicleId);
            List<DeliveryPart> toDeliveries = toSolution.get(vehicleId);

            // Skip if either is null (should not happen)
            if (fromDeliveries == null || toDeliveries == null)
                continue;

            // Count orders in each solution
            Map<String, Integer> fromOrderCounts = new HashMap<>();
            for (DeliveryPart part : fromDeliveries) {
                fromOrderCounts.put(part.getOrderId(),
                        fromOrderCounts.getOrDefault(part.getOrderId(), 0) + part.getGlpDeliverM3());
            }

            Map<String, Integer> toOrderCounts = new HashMap<>();
            for (DeliveryPart part : toDeliveries) {
                toOrderCounts.put(part.getOrderId(),
                        toOrderCounts.getOrDefault(part.getOrderId(), 0) + part.getGlpDeliverM3());
            }

            // Record differences
            for (String orderId : fromOrderCounts.keySet()) {
                int fromCount = fromOrderCounts.get(orderId);
                int toCount = toOrderCounts.getOrDefault(orderId, 0);
                if (fromCount != toCount) {
                    sb.append(vehicleId).append(":")
                            .append(orderId).append(":")
                            .append(fromCount).append("->")
                            .append(toCount).append(";");
                }
            }

            // Check for orders in toSolution that aren't in fromSolution
            for (String orderId : toOrderCounts.keySet()) {
                if (!fromOrderCounts.containsKey(orderId)) {
                    sb.append(vehicleId).append(":")
                            .append(orderId).append(":")
                            .append("0->")
                            .append(toOrderCounts.get(orderId)).append(";");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Check if a move is in the tabu list
     */
    private static boolean isTabuMove(String moveId, List<TabuMove> tabuList) {
        return tabuList.stream().anyMatch(tabu -> tabu.moveId.equals(moveId));
    }

    /**
     * Update the tabu list, reducing tenure and removing expired entries
     */
    private static void updateTabuList(List<TabuMove> tabuList) {
        Iterator<TabuMove> iterator = tabuList.iterator();
        while (iterator.hasNext()) {
            TabuMove move = iterator.next();
            if (move.decrementTenure()) {
                iterator.remove();
            }
        }
    }

    /**
     * Solves the vehicle routing problem using Tabu Search metaheuristic
     */
    public static Solution solve(SimulationState state) {
        // 1. INITIALIZATION
        Map<String, List<DeliveryPart>> currentAssignment = RandomDistributor.createInitialRandomAssignments(state);

        Solution currentSolution = SolutionGenerator.generateSolution(state, currentAssignment);
        Solution bestSolution = currentSolution;
        List<TabuMove> tabuList = new ArrayList<>();

        // 2. MAIN SEARCH LOOP
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // each 10%
            if (iteration % (MAX_ITERATIONS / 10) == 0) {
                System.out.println("Iteration " + iteration + ": " + bestSolution.getCost().totalCost());
                // try sort deliveries
                Map<String, List<DeliveryPart>> sortedAssignment = DistributionOperations
                        .sortDeliveries(bestSolution.getVehicleOrderAssignments(), state);
                Solution sortedSolution = SolutionGenerator.generateSolution(state, sortedAssignment);
                if (sortedSolution != null
                        && sortedSolution.getCost().totalCost() < bestSolution.getCost().totalCost()) {
                    currentAssignment = sortedAssignment;
                    currentSolution = sortedSolution;
                    bestSolution = sortedSolution;
                }
                // try greedySort deliveries
                Map<String, List<DeliveryPart>> greedySortedAssignment = DistributionOperations
                        .greedySort(bestSolution.getVehicleOrderAssignments(), state);
                Solution greedySortedSolution = SolutionGenerator.generateSolution(state, greedySortedAssignment);
                if (greedySortedSolution != null
                        && greedySortedSolution.getCost().totalCost() < bestSolution.getCost().totalCost()) {
                    currentAssignment = greedySortedAssignment;
                    currentSolution = greedySortedSolution;
                    bestSolution = greedySortedSolution;
                }
            }

            // a. Generate and evaluate the neighborhood of the current solution
            List<Map<String, List<DeliveryPart>>> neighbors = generateNeighbors(currentAssignment, state,
                    NUM_NEIGHBORS);
            Map<String, List<DeliveryPart>> bestCandidate = null;
            Solution bestCandidateSolution = null;

            // b. Find the best non-tabu neighbor
            for (Map<String, List<DeliveryPart>> neighbor : neighbors) {
                Solution neighborSolution = SolutionGenerator.generateSolution(state, neighbor);

                // Skip invalid solutions
                if (neighborSolution == null || neighborSolution.getCost().totalCost() == Double.POSITIVE_INFINITY) {
                    continue;
                }

                // Generate move ID for tabu checking
                String moveId = generateMoveId(currentAssignment, neighbor);
                boolean isTabu = isTabuMove(moveId, tabuList);

                // Aspiration criterion: accept tabu move if it's better than the best solution
                // so far
                boolean isAspirated = neighborSolution.getCost().totalCost() < bestSolution.getCost().totalCost();

                // Select the best permitted candidate
                if (!isTabu || isAspirated) {
                    if (bestCandidate == null ||
                            (bestCandidateSolution != null
                                    && neighborSolution.getCost().totalCost() < bestCandidateSolution.getCost()
                                            .totalCost())) {
                        bestCandidate = neighbor;
                        bestCandidateSolution = neighborSolution;
                    }
                }
            }

            // c. Make the move if a candidate was found
            if (bestCandidate != null && bestCandidateSolution != null) {
                // Move to the new solution
                currentAssignment = bestCandidate;
                currentSolution = bestCandidateSolution;

                // Update tabu memory: add the inverse move to the tabu list
                String moveId = generateMoveId(bestCandidate, currentAssignment);
                tabuList.add(new TabuMove(moveId, TABU_TENURE));

                // d. Update the best global solution
                if (currentSolution != null
                        && currentSolution.getCost().totalCost() < bestSolution.getCost().totalCost()) {
                    bestSolution = currentSolution;
                }
            }

            // Update tabu tenures and remove expired entries
            updateTabuList(tabuList);
        }

        // try sort deliveries
        Map<String, List<DeliveryPart>> sortedAssignment = DistributionOperations.sortDeliveries(
                bestSolution.getVehicleOrderAssignments(),
                state);
        Solution sortedSolution = SolutionGenerator.generateSolution(state, sortedAssignment);
        if (sortedSolution != null
                && sortedSolution.getCost().totalCost() < bestSolution.getCost().totalCost()) {
            currentAssignment = sortedAssignment;
            currentSolution = sortedSolution;
            bestSolution = sortedSolution;
        }

        // try greedySort deliveries
        Map<String, List<DeliveryPart>> greedySortedAssignment = DistributionOperations.greedySort(
                bestSolution.getVehicleOrderAssignments(),
                state);
        Solution greedySortedSolution = SolutionGenerator.generateSolution(state, greedySortedAssignment);
        if (greedySortedSolution != null
                && greedySortedSolution.getCost().totalCost() < bestSolution.getCost().totalCost()) {
            currentAssignment = greedySortedAssignment;
            currentSolution = greedySortedSolution;
            bestSolution = greedySortedSolution;
        }

        // 3. RETURN RESULT
        return bestSolution;
    }
}
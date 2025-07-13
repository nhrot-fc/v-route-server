package com.example.plgsystem.assignation;

import java.util.*;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.simulation.SimulationState;

public class MetaheuristicSolver {
    private static final Random random = new Random();

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
            Map<String, List<DeliveryPart>> neighbor;
            double rand = random.nextDouble();

            // Apply different operations with different probabilities
            if (rand < 0.2) {
                neighbor = DistributionOperations.shuffleSegment(currentAssignment);
            } else if (rand < 0.4) {
                neighbor = DistributionOperations.moveDelivery(currentAssignment);
            } else if (rand < 0.6) {
                neighbor = DistributionOperations.swapDeliveries(currentAssignment);
            } else if (rand < 0.8) {
                neighbor = DistributionOperations.moveDeliveryBetweenVehicles(currentAssignment);
            } else if (rand < 0.9) {
                neighbor = DistributionOperations.randomOperationWithState(currentAssignment, state);
            } else {
                neighbor = DistributionOperations.swapVehicles(currentAssignment);
            }

            neighbors.add(neighbor);
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
        for (int iteration = 0; iteration < Constants.MAX_ITERATIONS; iteration++) {
            // a. Generate and evaluate the neighborhood of the current solution
            List<Map<String, List<DeliveryPart>>> neighbors = generateNeighbors(currentAssignment, state,
                    Constants.NUM_NEIGHBORS);
            Map<String, List<DeliveryPart>> bestCandidate = null;
            Solution bestCandidateSolution = null;

            // b. Find the best non-tabu neighbor
            for (Map<String, List<DeliveryPart>> neighbor : neighbors) {
                Solution neighborSolution = SolutionGenerator.generateSolution(state, neighbor);

                // Skip invalid solutions
                if (neighborSolution.getCost() == Double.POSITIVE_INFINITY) {
                    continue;
                }

                // Generate move ID for tabu checking
                String moveId = generateMoveId(currentAssignment, neighbor);
                boolean isTabu = isTabuMove(moveId, tabuList);

                // Aspiration criterion: accept tabu move if it's better than the best solution
                // so far
                boolean isAspirated = neighborSolution.getCost() < bestSolution.getCost();

                // Select the best permitted candidate
                if (!isTabu || isAspirated) {
                    if (bestCandidate == null ||
                            (bestCandidateSolution != null
                                    && neighborSolution.getCost() < bestCandidateSolution.getCost())) {
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
                tabuList.add(new TabuMove(moveId, Constants.TABU_TENURE));

                // d. Update the best global solution
                if (currentSolution != null && currentSolution.getCost() < bestSolution.getCost()) {
                    bestSolution = currentSolution;
                }
            }

            // Update tabu tenures and remove expired entries
            updateTabuList(tabuList);
        }

        // 3. RETURN RESULT
        return bestSolution;
    }
}
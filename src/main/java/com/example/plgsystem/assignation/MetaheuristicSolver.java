package com.example.plgsystem.assignation;

import com.example.plgsystem.simulation.SimulationState;

import java.util.*;

public class MetaheuristicSolver {
    // Parámetros del algoritmo Tabú Search
    private static final int DEFAULT_MAX_ITERATIONS = 1500;
    private static final int DEFAULT_TABU_LIST_SIZE = 50;
    private static final int DEFAULT_NUM_NEIGHBORS = 50;

    // Parámetros del Simulated Annealing
    private static final double INITIAL_TEMPERATURE = 1000.0;
    private static final double COOLING_RATE = 0.97;
    private static final double FINAL_TEMPERATURE = 0.1;

    // Parámetros de optimización
    private static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 200;
    private static final double ACCEPTANCE_PROBABILITY_THRESHOLD = 0.01;

    public static Solution solve(SimulationState state) {
        // Generar solución inicial usando el distribuidor aleatorio
        Map<String, List<DeliveryPart>> initialAssignments = RandomDistributor.createInitialRandomAssignments(state);

        Solution currentSolution = SolutionGenerator.generateSolution(state, initialAssignments);
        currentSolution = SolutionEvaluator.evaluate(currentSolution, state);

        Solution bestSolution = currentSolution;

        // Inicializar lista tabú como una cola FIFO
        Queue<Double> tabuList = new LinkedList<>();

        // Inicialización de variables para Simulated Annealing
        double temperature = INITIAL_TEMPERATURE;
        Random random = new Random();
        int iterationsWithoutImprovement = 0;

        // Iteración principal
        for (int iteration = 0; iteration < DEFAULT_MAX_ITERATIONS && temperature > FINAL_TEMPERATURE; iteration++) {
            // Generar vecinos aplicando operaciones aleatorias
            List<Map<String, List<DeliveryPart>>> neighborAssignments = new ArrayList<>();
            for (int i = 0; i < DEFAULT_NUM_NEIGHBORS; i++) {
                neighborAssignments.add(
                        DistributionOperations.randomOperationWithState(
                                currentSolution.getRoutes().isEmpty() ? initialAssignments
                                        : currentSolution.getVehicleOrderAssignments(),
                                state));
            }

            // Evaluar vecinos y encontrar el mejor no tabú
            Solution bestNeighbor = null;
            double bestNeighborCost = Double.POSITIVE_INFINITY;

            for (Map<String, List<DeliveryPart>> assignments : neighborAssignments) {
                Solution neighbor = SolutionGenerator.generateSolution(state, assignments);
                neighbor = SolutionEvaluator.evaluate(neighbor, state);

                // Verificar si es tabú basado en su costo (simplificación)
                double neighborCost = neighbor.getCost();
                boolean isTabu = tabuList.contains(neighborCost);

                // Criterio de aspiración: aceptar soluciones tabú si son mejores que la mejor
                // global
                if (!isTabu || neighborCost < bestSolution.getCost()) {
                    if (neighborCost < bestNeighborCost) {
                        bestNeighbor = neighbor;
                        bestNeighborCost = neighborCost;
                    }
                }
            }

            // Si no encontramos un vecino válido, generamos uno aleatorio
            if (bestNeighbor == null) {
                Map<String, List<DeliveryPart>> randomAssignments = RandomDistributor
                        .createInitialRandomAssignments(state);
                bestNeighbor = SolutionGenerator.generateSolution(state, randomAssignments);
                bestNeighbor = SolutionEvaluator.evaluate(bestNeighbor, state);
                bestNeighborCost = bestNeighbor.getCost();
            }

            // Criterio de aceptación de Simulated Annealing
            boolean acceptMove = false;

            if (bestNeighborCost <= currentSolution.getCost()) {
                // Siempre aceptar mejoras
                acceptMove = true;
            } else {
                // Aceptar empeoramientos con una probabilidad que disminuye con la temperatura
                double delta = bestNeighborCost - currentSolution.getCost();
                double acceptanceProbability = Math.exp(-delta / temperature);

                if (acceptanceProbability > ACCEPTANCE_PROBABILITY_THRESHOLD &&
                        random.nextDouble() < acceptanceProbability) {
                    acceptMove = true;
                }
            }

            if (acceptMove) {
                currentSolution = bestNeighbor;

                // Añadir a lista tabú
                tabuList.offer(bestNeighborCost);
                while (tabuList.size() > DEFAULT_TABU_LIST_SIZE) {
                    tabuList.poll();
                }

                // Actualizar mejor solución global
                if (bestNeighborCost < bestSolution.getCost()) {
                    bestSolution = bestNeighbor;
                    iterationsWithoutImprovement = 0;
                } else {
                    iterationsWithoutImprovement++;
                }
            } else {
                iterationsWithoutImprovement++;
            }

            // Criterio de parada anticipada
            if (iterationsWithoutImprovement >= MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                break;
            }

            // Esquema de enfriamiento
            temperature *= COOLING_RATE;
        }

        return bestSolution;
    }
}

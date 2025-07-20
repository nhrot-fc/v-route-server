package com.example.plgsystem.demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.assignation.SolutionCost;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

/**
 * Experimento simplificado para evaluar el rendimiento del algoritmo de
 * optimización
 * con diferentes cantidades de órdenes.
 */
public class AlgorithmExperiment {
    // Parámetros para el experimento
    private static final int[] ORDER_COUNTS = { 10, 20, 30, 40, 50, 75, 100, 150, 200 };
    private static final int REPETITIONS = 5;
    private static final long RANDOM_SEED = 42L; // Seed fija para reproducibilidad

    // Parámetros del Tabú Search que se pueden variar
    private static final int[] MAX_ITERATIONS_VALUES = { 1000 };
    private static final int[] TABU_TENURE_VALUES = { 20 };
    private static final int[] NUM_NEIGHBORS_VALUES = { 15 };

    // Formato para el archivo de resultados
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RESULTS_DIRECTORY = "experiment_results";
    private static final String CSV_HEADER = "orders,max_iterations,tabu_tenure,num_neighbors,execution_time_ms," +
            "total_cost,time_cost,distance_cost,late_delivery_cost,incomplete_order_cost,invalid_cost,run\n";

    public static void main(String[] args) {
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);

        // Crear directorio para resultados si no existe
        try {
            Files.createDirectories(Paths.get(RESULTS_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Error al crear directorio de resultados: " + e.getMessage());
            return;
        }

        String resultsFilePath = RESULTS_DIRECTORY + "/algorithm_results_" +
                LocalDateTime.now().format(DATE_FORMATTER) + ".csv";

        String solutionsFilePath = RESULTS_DIRECTORY + "/algorithm_solutions_" +
                LocalDateTime.now().format(DATE_FORMATTER) + ".txt";

        System.out.println("Iniciando experimentos...");
        System.out.println("Los resultados se guardarán en: " + resultsFilePath);
        System.out.println("Las soluciones se guardarán en: " + solutionsFilePath);

        try (BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFilePath));
                BufferedWriter solutionsWriter = new BufferedWriter(new FileWriter(solutionsFilePath))) {

            // Escribir encabezado del CSV
            resultsWriter.write(CSV_HEADER);

            // Por defecto, usar valores estándar
            int defaultMaxIterations = Constants.MAX_ITERATIONS;
            int defaultTabuTenure = Constants.TABU_TENURE;
            int defaultNumNeighbors = Constants.NUM_NEIGHBORS;

            // Ejecutar experimentos con órdenes variadas y parámetros por defecto
            for (int orderCount : ORDER_COUNTS) {
                for (int run = 1; run <= REPETITIONS; run++) {
                    System.out.printf("Ejecutando: Órdenes=%d, Repetición=%d\n", orderCount, run);

                    try {
                        ExperimentResult result = runExperiment(
                                orderCount, startTime, 
                                defaultMaxIterations, 
                                defaultTabuTenure,
                                defaultNumNeighbors);
                                
                        if (result != null) {
                            // Guardar resultados numéricos en CSV con desglose de costos
                            writeResultToCSV(resultsWriter, result, run);
                            
                            // Guardar la solución completa en archivo de texto
                            solutionsWriter.write(String.format(
                                    "===== EXPERIMENTO: %d ÓRDENES - REPETICIÓN %d - MAX_ITER %d - TABU_TENURE %d - NUM_NEIGHBORS %d =====\n",
                                    orderCount, run, defaultMaxIterations, defaultTabuTenure, defaultNumNeighbors));
                            solutionsWriter.write(result.solutionDetails);
                            solutionsWriter.write("\n\n");
                            solutionsWriter.flush();
                        }
                    } catch (Exception e) {
                        System.err.println("Error en el experimento: " + e.getMessage());
                        e.printStackTrace();
                        resultsWriter.write(String.format(
                                "%d,%d,%d,%d,0,0.00,0.00,0.00,0.00,0.00,0.00,%d,ERROR\n", 
                                orderCount, defaultMaxIterations, defaultTabuTenure, defaultNumNeighbors, run));
                        resultsWriter.flush();
                        System.out.println("Continuando con el siguiente experimento...");
                    }
                }
            }
            
            // Experimentos adicionales variando parámetros del tabú search
            // (solo para algunos tamaños de órdenes representativos)
            int[] representativeOrderCounts = { 30, 100 };
            
            for (int orderCount : representativeOrderCounts) {
                // Variando MAX_ITERATIONS
                for (int maxIterations : MAX_ITERATIONS_VALUES) {
                    for (int run = 1; run <= REPETITIONS; run++) {
                        System.out.printf("Experimento parámetros: Órdenes=%d, MAX_ITER=%d, Rep=%d\n", 
                                orderCount, maxIterations, run);
                        
                        try {
                            ExperimentResult result = runExperiment(
                                    orderCount, startTime, 
                                    maxIterations, 
                                    defaultTabuTenure,
                                    defaultNumNeighbors);
                                    
                            if (result != null) {
                                writeResultToCSV(resultsWriter, result, run);
                                
                                solutionsWriter.write(String.format(
                                        "===== EXPERIMENTO PARÁMETROS (MAX_ITERATIONS): %d ÓRDENES - MAX_ITER %d - REP %d =====\n",
                                        orderCount, maxIterations, run));
                                solutionsWriter.write(result.solutionDetails);
                                solutionsWriter.write("\n\n");
                                solutionsWriter.flush();
                            }
                        } catch (Exception e) {
                            System.err.println("Error en el experimento: " + e.getMessage());
                            resultsWriter.write(String.format(
                                    "%d,%d,%d,%d,0,0.00,0.00,0.00,0.00,0.00,0.00,%d,ERROR\n", 
                                    orderCount, maxIterations, defaultTabuTenure, defaultNumNeighbors, run));
                            resultsWriter.flush();
                        }
                    }
                }
                
                // Variando TABU_TENURE
                for (int tabuTenure : TABU_TENURE_VALUES) {
                    for (int run = 1; run <= REPETITIONS; run++) {
                        System.out.printf("Experimento parámetros: Órdenes=%d, TABU_TENURE=%d, Rep=%d\n", 
                                orderCount, tabuTenure, run);
                        
                        try {
                            ExperimentResult result = runExperiment(
                                    orderCount, startTime, 
                                    defaultMaxIterations, 
                                    tabuTenure,
                                    defaultNumNeighbors);
                                    
                            if (result != null) {
                                writeResultToCSV(resultsWriter, result, run);
                                
                                solutionsWriter.write(String.format(
                                        "===== EXPERIMENTO PARÁMETROS (TABU_TENURE): %d ÓRDENES - TABU_TENURE %d - REP %d =====\n",
                                        orderCount, tabuTenure, run));
                                solutionsWriter.write(result.solutionDetails);
                                solutionsWriter.write("\n\n");
                                solutionsWriter.flush();
                            }
                        } catch (Exception e) {
                            System.err.println("Error en el experimento: " + e.getMessage());
                            resultsWriter.write(String.format(
                                    "%d,%d,%d,%d,0,0.00,0.00,0.00,0.00,0.00,0.00,%d,ERROR\n", 
                                    orderCount, defaultMaxIterations, tabuTenure, defaultNumNeighbors, run));
                            resultsWriter.flush();
                        }
                    }
                }
                
                // Variando NUM_NEIGHBORS
                for (int numNeighbors : NUM_NEIGHBORS_VALUES) {
                    for (int run = 1; run <= REPETITIONS; run++) {
                        System.out.printf("Experimento parámetros: Órdenes=%d, NUM_NEIGHBORS=%d, Rep=%d\n", 
                                orderCount, numNeighbors, run);
                        
                        try {
                            ExperimentResult result = runExperiment(
                                    orderCount, startTime, 
                                    defaultMaxIterations, 
                                    defaultTabuTenure,
                                    numNeighbors);
                                    
                            if (result != null) {
                                writeResultToCSV(resultsWriter, result, run);
                                
                                solutionsWriter.write(String.format(
                                        "===== EXPERIMENTO PARÁMETROS (NUM_NEIGHBORS): %d ÓRDENES - NUM_NEIGHBORS %d - REP %d =====\n",
                                        orderCount, numNeighbors, run));
                                solutionsWriter.write(result.solutionDetails);
                                solutionsWriter.write("\n\n");
                                solutionsWriter.flush();
                            }
                        } catch (Exception e) {
                            System.err.println("Error en el experimento: " + e.getMessage());
                            resultsWriter.write(String.format(
                                    "%d,%d,%d,%d,0,0.00,0.00,0.00,0.00,0.00,0.00,%d,ERROR\n", 
                                    orderCount, defaultMaxIterations, defaultTabuTenure, numNeighbors, run));
                            resultsWriter.flush();
                        }
                    }
                }
            }

            System.out.println("Experimentos completados. Resultados guardados en " + resultsFilePath);

        } catch (IOException e) {
            System.err.println("Error al escribir resultados: " + e.getMessage());
        }
    }

    private static void writeResultToCSV(BufferedWriter writer, ExperimentResult result, int run) throws IOException {
        writer.write(String.format("%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d\n",
                result.orders,
                result.maxIterations,
                result.tabuTenure,
                result.numNeighbors,
                result.executionTimeMs,
                result.totalCost,
                result.timeCost,
                result.distanceCost,
                result.lateDeliveryCost,
                result.incompleteOrderCost,
                result.invalidCost,
                run));
        writer.flush();
    }

    private static SimulationState createSimulationState(LocalDateTime startTime) {
        List<Vehicle> vehicles = new ArrayList<>();
        List<Depot> auxDepots = new ArrayList<>();
        Depot mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
        auxDepots.add(
                new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 160, DepotType.AUXILIARY));
        auxDepots.add(
                new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 160, DepotType.AUXILIARY));

        for (int i = 1; i <= 2; i++) {
            vehicles.add(new Vehicle(String.format("TA%02d", i), VehicleType.TA, Constants.MAIN_DEPOT_LOCATION));
        }
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TB%02d", i), VehicleType.TB, Constants.MAIN_DEPOT_LOCATION));
        }
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TC%02d", i), VehicleType.TC, Constants.MAIN_DEPOT_LOCATION));
        }
        for (int i = 1; i <= 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, Constants.MAIN_DEPOT_LOCATION));
        }

        return new SimulationState(vehicles, mainDepot, auxDepots, startTime);
    }

    private static List<Order> generateRandomOrders(int count, LocalDateTime startTime) {
        Random random = new Random(RANDOM_SEED);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String orderId = "order_" + (i + 1);
            int remainingGlpM3 = random.nextInt(50) + 1; // Entre 1 y 50 m³
            int limitHours = random.nextInt(20) + 4; // Entre 4 y 24 horas
            LocalDateTime deliveryTime = startTime.plusHours(limitHours);
            Position position = new Position(random.nextInt(100), random.nextInt(100)); // Coordenadas aleatorias
            orders.add(new Order(orderId, startTime, deliveryTime, remainingGlpM3, position));
        }
        return orders;
    }

    private static ExperimentResult runExperiment(int orderCount, LocalDateTime startTime, 
                                               int maxIterations, int tabuTenure, int numNeighbors) {
        // Generar pedidos aleatorios
        List<Order> orders = generateRandomOrders(orderCount, startTime);

        // Crear estado de simulación inicial
        SimulationState state = createSimulationState(startTime);
        for (Order order : orders) {
            state.addOrder(order);
        }

        // Configurar parámetros del solver
        MetaheuristicSolver.configure(maxIterations, tabuTenure, numNeighbors);

        // Resolver el problema de asignación
        long startExecution = System.currentTimeMillis();
        Solution solution = MetaheuristicSolver.solve(state);
        long endExecution = System.currentTimeMillis();
        
        // Preparar detalles de la solución
        String solutionDetails = solution.toString();
        SolutionCost cost = solution.getCost();

        return new ExperimentResult(orderCount,
                maxIterations,
                tabuTenure,
                numNeighbors,
                endExecution - startExecution,
                cost.totalCost(),
                cost.timeCost(),
                cost.distanceCost(),
                cost.lateDeliveryCost(),
                cost.incompleteOrderCost(),
                cost.invalidCost(),
                solutionDetails);
    }

    private record ExperimentResult(
            int orders,
            int maxIterations,
            int tabuTenure,
            int numNeighbors,
            long executionTimeMs,
            double totalCost,
            double timeCost,
            double distanceCost,
            double lateDeliveryCost,
            double incompleteOrderCost,
            double invalidCost,
            String solutionDetails) {

        @Override
        public String toString() {
            return String.format("Órdenes: %d, Tiempo: %d ms, Costo Total: %.2f " +
                    "(Tiempo: %.2f, Distancia: %.2f, Retraso: %.2f, Incompleto: %.2f, Inválido: %.2f)",
                    orders, executionTimeMs, totalCost, timeCost, distanceCost, 
                    lateDeliveryCost, incompleteOrderCost, invalidCost);
        }
    }
}

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
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.simulation.SimulationState;

/**
 * Experimento para evaluar el rendimiento del algoritmo de optimización
 * con diferentes cantidades de órdenes y bloqueos.
 */
public class AlgorithmExperiment {
    // Parámetros para el experimento
    private static final int[] ORDER_COUNTS = { 5, 10, 20, 30, 50, 75, 100 };
    private static final int[] BLOCKAGE_COUNTS = { 10, 25, 50 };
    private static final int REPETITIONS = 3;
    private static final long RANDOM_SEED = 42L; // Seed fija para reproducibilidad

    // Formato para el archivo de resultados
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RESULTS_DIRECTORY = "experiment_results";
    private static final String CSV_HEADER = "orders,blockages,iterations,execution_time_ms,solution_cost,run\n";

    public static void main(String[] args) {
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = startTime.plusMonths(1);

        // Crear directorio para resultados si no existe
        try {
            Files.createDirectories(Paths.get(RESULTS_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Error al crear directorio de resultados: " + e.getMessage());
            return;
        }

        String resultsFilePath = RESULTS_DIRECTORY + "/algorithm_results_" +
                LocalDateTime.now().format(DATE_FORMATTER) + ".csv";

        System.out.println("Iniciando experimentos...");
        System.out.println("Los resultados se guardarán en: " + resultsFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFilePath))) {
            // Escribir encabezado del CSV
            writer.write(CSV_HEADER);

            // Ejecutar experimentos con diferentes configuraciones
            for (int orderCount : ORDER_COUNTS) {
                for (int blockageCount : BLOCKAGE_COUNTS) {
                    for (int run = 1; run <= REPETITIONS; run++) {
                        System.out.printf("Ejecutando: Órdenes=%d, Bloqueos=%d, Repetición=%d\n",
                                orderCount, blockageCount, run);

                        try {
                            ExperimentResult result = runExperiment(orderCount, blockageCount, startTime, endTime);
                            if (result != null) {
                                writer.write(String.format("%d,%d,%d,%d,%.2f,%d\n",
                                        result.orders, 
                                        result.blockages,
                                        Constants.MAX_ITERATIONS,
                                        result.executionTimeMs,
                                        result.solutionCost,
                                        run));
                                writer.flush(); // Guardar después de cada ejecución
                            }
                        } catch (Exception e) {
                            System.err.println("Error fatal en el experimento: " + e.getMessage());
                            e.printStackTrace();

                            // Registrar el error en el CSV de forma simplificada
                            String errorLine = String.format(
                                    "%d,%d,%d,0,0.00,%d,ERROR: %s\n",
                                    orderCount, blockageCount, Constants.MAX_ITERATIONS, 
                                    run, e.getMessage().replace(',', ';'));
                            writer.write(errorLine);
                            writer.flush();

                            // Continuar con el siguiente experimento en lugar de abortar todo
                            System.out.println("Continuando con el siguiente experimento...");
                        }
                    }
                }
            }

            System.out.println("Experimentos completados. Resultados guardados en " + resultsFilePath);

        } catch (IOException e) {
            System.err.println("Error al escribir resultados: " + e.getMessage());
        }
    }

    private static ExperimentResult runExperiment(int maxOrders, int maxBlockages,
            LocalDateTime startTime, LocalDateTime endTime) {
        SimulationState simulationState = createSimulationState(startTime);
        generateSyntheticData(simulationState, maxOrders, maxBlockages, startTime, endTime);
        System.out.println("Ejecutando algoritmo con " + simulationState.getOrders().size() +
                " órdenes y " + simulationState.getBlockages().size() + " bloqueos");

        // Medir tiempo de ejecución
        long startCrono = System.nanoTime();
        Solution solution = MetaheuristicSolver.solve(simulationState);
        long endCrono = System.nanoTime();

        // Convertir a milisegundos para mayor legibilidad
        long durationMs = (endCrono - startCrono) / 1_000_000;

        System.out.println("Solución encontrada con costo: " + solution.getCost() +
                " en " + durationMs + " ms");

        return new ExperimentResult(
                maxOrders,
                maxBlockages,
                durationMs,
                solution.getCost());
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

    /**
     * Genera datos sintéticos para pruebas cuando los archivos de datos reales no
     * están disponibles
     */
    private static void generateSyntheticData(SimulationState simulationState, int maxOrders, int maxBlockages,
            LocalDateTime startTime, LocalDateTime endTime) {

        Random random = new Random(RANDOM_SEED); // Semilla fija para reproducibilidad
        int cityX = Constants.CITY_X;
        int cityY = Constants.CITY_Y;

        // Limpiar datos anteriores
        simulationState.getOrders().clear();
        simulationState.getBlockages().clear();

        // Generar órdenes sintéticas
        for (int i = 0; i < maxOrders; i++) {
            // Posición aleatoria en la ciudad
            Position position = new Position(
                    random.nextInt(cityX),
                    random.nextInt(cityY));

            // Tiempo de llegada aleatorio dentro del rango de simulación
            long startSeconds = startTime.toEpochSecond(java.time.ZoneOffset.UTC);
            long endSeconds = endTime.toEpochSecond(java.time.ZoneOffset.UTC);
            long randomSeconds = startSeconds + random.nextInt((int) (endSeconds - startSeconds));

            LocalDateTime arrivalTime = LocalDateTime.ofEpochSecond(
                    randomSeconds, 0, java.time.ZoneOffset.UTC);

            // Tiempo límite de entre 4 y 24 horas después
            LocalDateTime deadlineTime = arrivalTime.plusHours(4 + random.nextInt(20));

            // Cantidad de GLP entre 1 y 20 m³
            int glpRequest = 1 + random.nextInt(20);

            // Crear orden sintética
            Order order = new Order(
                    "SYNTH-" + i,
                    arrivalTime,
                    deadlineTime,
                    glpRequest,
                    position);

            simulationState.addOrder(order);
        }

        // Generar bloqueos sintéticos
        for (int i = 0; i < maxBlockages; i++) {
            // Posición aleatoria en la ciudad
            Position position = new Position(
                    random.nextInt(cityX),
                    random.nextInt(cityY));

            // Tiempo de inicio aleatorio dentro del rango de simulación
            long startSeconds = startTime.toEpochSecond(java.time.ZoneOffset.UTC);
            long endSeconds = endTime.toEpochSecond(java.time.ZoneOffset.UTC);
            long randomStartSeconds = startSeconds + random.nextInt((int) (endSeconds - startSeconds));

            LocalDateTime blockageStart = LocalDateTime.ofEpochSecond(
                    randomStartSeconds, 0, java.time.ZoneOffset.UTC);

            // Duración del bloqueo entre 1 y 12 horas
            LocalDateTime blockageEnd = blockageStart.plusHours(1 + random.nextInt(12));
            if (blockageEnd.isAfter(endTime)) {
                blockageEnd = endTime;
            }

            // Crear línea de bloqueo con 2-4 puntos
            int numPoints = 2 + random.nextInt(3);
            List<Position> blockagePoints = new ArrayList<>();

            // Primer punto
            blockagePoints.add(position);

            // Puntos adicionales en un radio de 3-10 km
            for (int j = 1; j < numPoints; j++) {
                int offsetX = random.nextInt(10) - 5; // -5 a 5
                int offsetY = random.nextInt(10) - 5; // -5 a 5

                int newX = Math.max(0, Math.min(cityX, position.getX() + offsetX));
                int newY = Math.max(0, Math.min(cityY, position.getY() + offsetY));

                blockagePoints.add(new Position(newX, newY));
            }

            // Crear bloqueo sintético
            Blockage blockage = new Blockage(
                    blockageStart,
                    blockageEnd,
                    blockagePoints);

            simulationState.addBlockage(blockage);
        }

        System.out.println("Generados " + simulationState.getOrders().size() + " órdenes sintéticas");
        System.out.println("Generados " + simulationState.getBlockages().size() + " bloqueos sintéticos");
    }

    /**
     * Clase para almacenar los resultados de cada experimento
     */
    private static class ExperimentResult {
        private final int orders;
        private final int blockages;
        private final long executionTimeMs;
        private final double solutionCost;

        public ExperimentResult(int orders, int blockages, long executionTimeMs, double solutionCost) {
            this.orders = orders;
            this.blockages = blockages;
            this.executionTimeMs = executionTimeMs;
            this.solutionCost = solutionCost;
        }
    }
}

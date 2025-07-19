package com.example.plgsystem.model;

import java.time.format.DateTimeFormatter;

public class Constants {
    /*
     * =============================================
     * TIEMPOS Y DURACIONES
     * =============================================
     */
    // Tiempos en horas
    public static final int MAINTENANCE_DURATION_HOURS = 24;
    public static final int INCIDENT_TYPE_1_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_2_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_3_IMMOBILIZATION_HOURS = 4;
    public static final int MIN_PACKAGE_DELIVERY_TIME_HOURS = 4; // Initial suggested value

    // Tiempos en minutos
    public static final int GLP_SERVE_DURATION_MINUTES = 5;
    public static final int VEHICLE_GLP_TRANSFER_DURATION_MINUTES = 5;
    public static final int REFUEL_DURATION_MINUTES = 1;
    public static final int DEPOT_GLP_TRANSFER_TIME_MINUTES = 1;

    /*
     * =============================================
     * LOCALIZACIÓN Y CONFIGURACIÓN GEOGRÁFICA
     * =============================================
     */
    // City Configuration
    public static final int CITY_X = 70; // Km
    public static final int CITY_Y = 50; // Km
    public static final int NODE_DISTANCE = 1; // Km

    // Storage Locations
    public static final Position MAIN_DEPOT_LOCATION = new Position(12, 8);
    public static final Position NORTH_DEPOT_LOCATION = new Position(42, 42);
    public static final Position EAST_DEPOT_LOCATION = new Position(63, 3);

    /*
     * =============================================
     * CAPACIDADES Y CARACTERÍSTICAS DE VEHÍCULOS
     * =============================================
     */
    // Vehículos - General
    public static final double VEHICLE_FUEL_CAPACITY_GAL = 25.0; // Gallons
    public static final double VEHICLE_AVG_SPEED = 120.0; // Km/h
    public static final String VEHICLE_CODE_FORMAT = "TTNN";
    public static final double CONSUMPTION_FACTOR = 360.0;

    // Capacidades GLP
    public static final int TA_GLP_CAPACITY_M3 = 25; // m³
    public static final int TB_GLP_CAPACITY_M3 = 15; // m³
    public static final int TC_GLP_CAPACITY_M3 = 10; // m³
    public static final int TD_GLP_CAPACITY_M3 = 5; // m³

    // Cantidades de vehículos
    public static final int TA_UNITS = 2;
    public static final int TB_UNITS = 4;
    public static final int TC_UNITS = 4;
    public static final int TD_UNITS = 10;

    /*
     * =============================================
     * PESOS Y MEDIDAS
     * =============================================
     */
    public static final double TA_GROSS_WEIGHT_TARA_TON = 2.5; // Ton
    public static final double TB_GROSS_WEIGHT_TARA_TON = 2.0; // Ton
    public static final double TC_GROSS_WEIGHT_TARA_TON = 1.5; // Ton
    public static final double TD_GROSS_WEIGHT_TARA_TON = 1.0; // Ton
    public static final double GLP_DENSITY_M3_TON = 0.5;
    // Constantes matemáticas
    public static final double EPSILON = 1e-9; // For floating-point comparisons

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss");


    // Metaheuristic Solver Constants
    public static final int MAX_ITERATIONS = 1500;
    public static final int TABU_TENURE = 20;
    public static final int NUM_NEIGHBORS = 15;

    private Constants() {
        // Avoid initialization
    }
}

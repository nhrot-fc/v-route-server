package com.example.plgsystem.model;

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
    public static final int MIN_PACKAGE_DELIVERY_TIME_HOURS = 4;

    // Tiempos en minutos
    public static final int GLP_SERVE_DURATION_MINUTES = 15;
    public static final int VEHICLE_GLP_TRANSFER_DURATION_MINUTES = 15;
    public static final int REFUEL_DURATION_MINUTES = 20;
    public static final int DEPOT_GLP_TRANSFER_TIME_MINUTES = 15;
    public static final int ROUTINE_MAINTENANCE_MINUTES = 15;
    public static final int CUSTOMER_DELIVERY_UNLOAD_TIME_MINUTES = 15;

    /*
     * =============================================
     * LOCALIZACIÓN Y CONFIGURACIÓN GEOGRÁFICA
     * =============================================
     */
    public static final double CITY_LENGTH_X = 70.0; // Km
    public static final double CITY_WIDTH_Y = 50.0; // Km
    public static final double NODE_DISTANCE = 1.0; // Km

    // Storage Locations
    public static final Position CENTRAL_STORAGE_LOCATION = new Position(12, 8);
    public static final Position NORTH_INTERMEDIATE_STORAGE_LOCATION = new Position(42, 42);
    public static final Position EAST_INTERMEDIATE_STORAGE_LOCATION = new Position(63, 3);

    /*
     * =============================================
     * CAPACIDADES Y CARACTERÍSTICAS DE VEHÍCULOS
     * =============================================
     */
    public static final double VEHICLE_FUEL_CAPACITY_GAL = 25.0;
    public static final double VEHICLE_AVG_SPEED = 50.0;
    public static final String VEHICLE_CODE_FORMAT = "TTNN";
    public static final double CONSUMPTION_FACTOR = 360.0;

    // Capacidades GLP
    public static final int TA_GLP_CAPACITY_M3 = 25;
    public static final int TB_GLP_CAPACITY_M3 = 15;
    public static final int TC_GLP_CAPACITY_M3 = 10;
    public static final int TD_GLP_CAPACITY_M3 = 5;

    // Cantidades de vehículos
    public static final int TA_UNITS = 2;
    public static final int TB_UNITS = 4;
    public static final int TC_UNITS = 4;
    public static final int TD_UNITS = 10;

    public static final double TOTAL_FLEET_LPG_CAPACITY = 200.0;

    /*
     * =============================================
     * PESOS Y MEDIDAS
     * =============================================
     */
    public static final double TA_GROSS_WEIGHT_TARA_TON = 2.5;
    public static final double TA_GLP_WEIGHT_TON = 12.5;
    public static final double TA_COMBINED_WEIGHT_TON = 15.0;

    public static final double TB_GROSS_WEIGHT_TARA_TON = 2.0;
    public static final double TB_GLP_WEIGHT_TON = 7.5;
    public static final double TB_COMBINED_WEIGHT_TON = 9.5;

    public static final double TC_GROSS_WEIGHT_TARA_TON = 1.5;
    public static final double TC_GLP_WEIGHT_TON = 5.0;
    public static final double TC_COMBINED_WEIGHT_TON = 6.5;

    public static final double TD_GROSS_WEIGHT_TARA_TON = 1.0;
    public static final double TD_GLP_WEIGHT_TON = 2.5;
    public static final double TD_COMBINED_WEIGHT_TON = 3.5;

    /*
     * =============================================
     * FORMATOS Y CONFIGURACIÓN DE ARCHIVOS
     * =============================================
     */
    public static final String PREVENTIVE_MAINTENANCE_FILE_NAME = "mantpreventivo";
    public static final String PREVENTIVE_MAINTENANCE_FILE_FORMAT = "aaaammdd:TTNN";

    public static final String ORDER_FILE_BASE_NAME = "ventas2025mm";
    public static final String ORDER_FILE_FORMAT = "##d##h##m:posx,posY,c-idCliente, m3, hLímite";

    public static final String INCIDENT_FILE_NAME = "averias.txt";
    public static final String INCIDENT_FILE_FORMAT = "tt_######_ti";

    public static final String STREET_CLOSURE_FILE_BASE_NAME = "aaaamm.bloqueadas";

    /*
     * =============================================
     * PARÁMETROS OPERACIONALES
     * =============================================
     */
    public static final double INCIDENT_ROUTE_OCCURRENCE_MIN_PERCENTAGE = 0.05;
    public static final double INCIDENT_ROUTE_OCCURRENCE_MAX_PERCENTAGE = 0.35;

    /*
     * =============================================
     * IDENTIFICADORES Y CONSTANTES MATEMÁTICAS
     * =============================================
     */
    public static final String MAIN_PLANT_ID = "MAIN_PLANT";
    public static final double EPSILON = 1e-9;

    private Constants() {
        // Prevent instantiation
    }
}

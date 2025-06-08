package com.example.plgsystem.model;

public class Constants {
    // vehicle constants
    public static final double VEHICLE_FUEL_CAPACITY = 25.0; // Gallons
    public static final double VEHICLE_AVG_SPEED = 50.0; // Km/h
    public static final double VEHICLE_MAINTENANCE_DURATION_HOURS = 24.0;
    public static final double VEHICLE_STORAGE_CHECK_DURATION_MINUTES = 15.0;
    public static final double VEHICLE_TO_VEHICLE_LOAD_TRANSFER_DURATION_MINUTES = 15.0;
    public static final int CONSUMPTION_FACTOR = 180;

    // order constants
    public static final double ORDER_SERVE_DURATION_MINUTES = 15.0;
    public static final double REFILL_DURATION_MINUTES = 20.0;

    // math constants
    public static final double EPSILON = 1e-9; // For floating-point comparisons

    // City Configuration
    public static final double CITY_LENGTH_X = 70.0; // Km
    public static final double CITY_WIDTH_Y = 50.0; // Km
    public static final double NODE_DISTANCE = 1.0; // Km

    // Storage Locations
    public static final Position CENTRAL_STORAGE_LOCATION = new Position(12, 8);
    public static final Position NORTH_INTERMEDIATE_STORAGE_LOCATION = new Position(42, 42);
    public static final Position EAST_INTERMEDIATE_STORAGE_LOCATION = new Position(63, 3);

    // Street Closures
    public static final String STREET_CLOSURE_FILE_BASE_NAME = "aaaamm.bloqueadas";

    // Vehicle Fleet - General
    public static final String VEHICLE_CODE_FORMAT = "TTNN";

    // Vehicle Fleet - Type TA
    public static final double TA_GROSS_WEIGHT_TARA = 2.5; // Ton
    public static final double TA_LPG_CAPACITY = 25.0; // m³
    public static final double TA_LPG_WEIGHT = 12.5; // Ton
    public static final double TA_COMBINED_WEIGHT = 15.0; // Ton
    public static final int TA_UNITS = 2;

    // Vehicle Fleet - Type TB
    public static final double TB_GROSS_WEIGHT_TARA = 2.0; // Ton
    public static final double TB_LPG_CAPACITY = 15.0; // m³
    public static final double TB_LPG_WEIGHT = 7.5; // Ton
    public static final double TB_COMBINED_WEIGHT = 9.5; // Ton
    public static final int TB_UNITS = 4;

    // Vehicle Fleet - Type TC
    public static final double TC_GROSS_WEIGHT_TARA = 1.5; // Ton
    public static final double TC_LPG_CAPACITY = 10.0; // m³
    public static final double TC_LPG_WEIGHT = 5.0; // Ton
    public static final double TC_COMBINED_WEIGHT = 6.5; // Ton
    public static final int TC_UNITS = 4;

    // Vehicle Fleet - Type TD
    public static final double TD_GROSS_WEIGHT_TARA = 1.0; // Ton
    public static final double TD_LPG_CAPACITY = 5.0; // m³
    public static final double TD_LPG_WEIGHT = 2.5; // Ton
    public static final double TD_COMBINED_WEIGHT = 3.5; // Ton
    public static final int TD_UNITS = 10;

    public static final double TOTAL_FLEET_LPG_CAPACITY = 200.0; // m³

    // Preventive Maintenance
    public static final String PREVENTIVE_MAINTENANCE_FILE_NAME = "mantpreventivo";
    public static final String PREVENTIVE_MAINTENANCE_FILE_FORMAT = "aaaammdd:TTNN";
    public static final int PREVENTIVE_MAINTENANCE_UNAVAILABILITY_HOURS = 24; // From 00:00 to 23:59

    // Orders
    public static final String ORDER_FILE_BASE_NAME = "ventas2025mm";
    public static final String ORDER_FILE_FORMAT = "##d##h##m:posx,posY,c-idCliente, m3, hLímite";

    // Incidents
    public static final int INCIDENT_TYPE_1_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_2_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_3_IMMOBILIZATION_HOURS = 4;
    public static final String INCIDENT_FILE_NAME = "averias.txt";
    public static final String INCIDENT_FILE_FORMAT = "tt_######_ti";
    public static final double INCIDENT_ROUTE_OCCURRENCE_MIN_PERCENTAGE = 0.05;
    public static final double INCIDENT_ROUTE_OCCURRENCE_MAX_PERCENTAGE = 0.35;
    public static final double PRODUCT_TRANSFER_TIME_MINUTES = 15.0;

    // Operations and Times
    public static final double PLANT_LOAD_UNLOAD_TIME_MINUTES = 0.0;
    public static final double CUSTOMER_DELIVERY_UNLOAD_TIME_MINUTES = 15.0;
    public static final double PLANT_ROUTINE_MAINTENANCE_TIME_MINUTES = 15.0;

    // Algorithm Parametrization
    public static final double MINIMUM_PACKAGE_DELIVERY_TIME_HOURS = 4.0; // Initial suggested value
}

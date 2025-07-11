package com.example.plgsystem.exceptions;

/**
 * Exception thrown when operations related to vehicles are invalid.
 * This could be due to non-existent vehicles, vehicles with invalid state,
 * or vehicles that cannot perform the requested operation.
 */
public class InvalidVehicleException extends Exception {
    public InvalidVehicleException(String message) {
        super(message);
    }
} 
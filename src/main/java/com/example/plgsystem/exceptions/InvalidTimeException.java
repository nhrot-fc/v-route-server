package com.example.plgsystem.exceptions;

/**
 * Exception thrown when operations related to time are invalid.
 * This could be due to attempting to schedule actions in the past,
 * invalid time ranges, or inconsistent time sequences in plans.
 */
public class InvalidTimeException extends Exception {
    public InvalidTimeException(String message) {
        super(message);
    }
} 
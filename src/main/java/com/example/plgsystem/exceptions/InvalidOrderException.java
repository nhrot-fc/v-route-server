package com.example.plgsystem.exceptions;

/**
 * Exception thrown when operations related to orders are invalid.
 * This could be due to non-existent orders, attempting to deliver to
 * already completed orders, or delivering invalid amounts of GLP.
 */
public class InvalidOrderException extends Exception {
    public InvalidOrderException(String message) {
        super(message);
    }
} 
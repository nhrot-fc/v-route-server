package com.example.plgsystem.exceptions;

/**
 * Exception thrown when a vehicle has insufficient GLP to complete a delivery
 * or when a depot has insufficient GLP for loading a vehicle.
 */
public class InsufficientGLPException extends Exception {
    public InsufficientGLPException(String message) {
        super(message);
    }
} 
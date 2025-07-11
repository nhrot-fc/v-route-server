package com.example.plgsystem.exceptions;

/**
 * Exception thrown when trying to operate on a route with no stops.
 * This could indicate a planning or validation error earlier in the process.
 */
public class EmptyRouteException extends Exception {
    public EmptyRouteException(String message) {
        super(message);
    }
} 
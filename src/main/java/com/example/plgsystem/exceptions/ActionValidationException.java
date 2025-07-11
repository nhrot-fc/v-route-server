package com.example.plgsystem.exceptions;

/**
 * Exception thrown when an action fails validation.
 * This could be due to invalid parameters, conflicting actions,
 * or actions that cannot be executed given the current state.
 */
public class ActionValidationException extends Exception {
    public ActionValidationException(String message) {
        super(message);
    }
} 
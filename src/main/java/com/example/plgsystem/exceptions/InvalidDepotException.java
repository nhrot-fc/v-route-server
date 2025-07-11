package com.example.plgsystem.exceptions;

/**
 * Exception thrown when operations related to depots are invalid.
 * This could be due to non-existent depots, depots of wrong type,
 * or attempting operations not supported by a particular depot type.
 */
public class InvalidDepotException extends Exception {
    public InvalidDepotException(String message) {
        super(message);
    }
} 
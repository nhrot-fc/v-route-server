package com.example.plgsystem.operation;

/**
 * Defines the types of actions that vehicles can perform during the simulation.
 */
public enum ActionType {
    /** Moving from one position to another */
    DRIVE,
    
    /** Reloading GLP from a depot */
    RELOAD,
    
    /** Delivering GLP to a customer */
    SERVE,
    
    /** Performing maintenance on the vehicle */
    MAINTENANCE,
    
    /** Waiting at a location (idle time) */
    WAIT
}
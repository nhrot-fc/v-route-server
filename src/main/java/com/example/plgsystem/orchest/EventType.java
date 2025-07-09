package com.example.plgsystem.orchest;

/**
 * Represents the type of events that can occur in the simulation.
 */
public enum EventType {
    /** A new order has arrived in the system */
    ORDER_ARRIVAL,
    
    /** A street blockage has started */
    BLOCKAGE_START,
    
    /** A street blockage has ended */
    BLOCKAGE_END,
    
    /** A vehicle has broken down */
    VEHICLE_BREAKDOWN,
    
    /** A vehicle has started maintenance */
    MAINTENANCE_START,
    
    /** A vehicle has ended maintenance */
    MAINTENANCE_END,
    
    /** New day has begun */
    NEW_DAY_BEGIN,
    
    /** The simulation has reached its end time */
    SIMULATION_END
}
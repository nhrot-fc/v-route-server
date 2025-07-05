package com.example.plgsystem.model;

/**
 * Enumeration of possible status values for vehicles.
 * Each status corresponds to one or more vehicle actions.
 */
public enum VehicleStatus {
    AVAILABLE("✅"),       // Ready for a new task
    DRIVING("🚗"),         // Driving to a location
    SERVING("🛒"),         // Serving an order
    MAINTENANCE("🔧"),     // Under maintenance
    REFUELING("⛽"),        // Refueling (if applies)
    RELOADING("🛢️"),       // Reloading GLP
    IDLE("⏸️"),            // Waiting for task
    UNAVAILABLE("🚫"),     // Not usable (incidents, etc.)
    BROKEN_DOWN("💥");     // Optional if still used in your logic

    private final String icon;

    VehicleStatus(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return icon + " " + name();
    }
}

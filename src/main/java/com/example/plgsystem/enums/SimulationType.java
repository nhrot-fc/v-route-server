package com.example.plgsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SimulationType {
    // Daily operations - affects database
    DAILY_OPERATIONS("📋", "Daily Operations"),

    // Simulation types (memory only)
    WEEKLY("🔄", "Weekly Simulation"),
    INFINITE("♾️", "Infinite Simulation"),
    CUSTOM("⚙️", "Custom Simulation");

    private final String icon;
    private final String description;

    public boolean isDailyOperation() {
        return this == DAILY_OPERATIONS;
    }

    public boolean isTimeBasedSimulation() {
        return this == WEEKLY || this == INFINITE || this == CUSTOM;
    }
}

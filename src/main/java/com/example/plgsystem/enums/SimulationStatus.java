package com.example.plgsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SimulationStatus {
    RUNNING("🟢", "Running"),
    PAUSED("🟡", "Paused"),
    FINISHED("🔵", "Finished"),
    ERROR("🔴", "Error");

    private final String icon;
    private final String description;
}
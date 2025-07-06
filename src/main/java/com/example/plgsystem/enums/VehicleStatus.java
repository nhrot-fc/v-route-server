package com.example.plgsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeración que define los estados posibles de un vehículo
 */
@Getter
@RequiredArgsConstructor
public enum VehicleStatus {
    AVAILABLE("✅", "Disponible"),
    DRIVING("🚚", "En ruta"),
    MAINTENANCE("🔧", "En mantenimiento"),
    REFUELING("⛽", "Repostando"),
    RELOADING("🔄", "Cargando GLP"),
    SERVING("📦", "Entregando"),
    INCIDENT("🚨", "Con incidente"),
    IDLE("🛑", "Ocioso");

    private final String icon;
    private final String description;
}

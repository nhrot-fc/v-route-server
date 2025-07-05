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
    IN_ROUTE("🚚", "En ruta"),
    MAINTENANCE("🔧", "En mantenimiento"),
    REFUELING("⛽", "Repostando"),
    LOADING("🔄", "Cargando GLP"),
    DELIVERING("📦", "Entregando"),
    INCIDENT("🚨", "Con incidente");

    private final String icon;
    private final String description;
}

package com.example.plgsystem.model;

/**
 * Interfaz que representa una parada o punto de interés en el sistema
 */
public interface Stop {
    /**
     * Obtiene la posición geográfica de la parada
     * @return Posición de la parada
     */
    Position getPosition();
    /**
     * Obtiene el ID único de la parada
     * @return ID de la parada
     */
    String getId();
}

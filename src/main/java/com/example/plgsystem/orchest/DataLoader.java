package com.example.plgsystem.orchest;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz que define las operaciones de carga de datos para la simulación.
 * Las implementaciones pueden cargar datos desde distintas fuentes (archivos, base de datos, etc.)
 */
public interface DataLoader {
    
    /**
     * Carga órdenes para una fecha específica
     * 
     * @param date La fecha para la que cargar órdenes
     * @return Lista de eventos generados a partir de las órdenes
     */
    List<Event> loadOrdersForDate(LocalDate date);
    
    /**
     * Carga bloqueos para una fecha específica
     * 
     * @param date La fecha para la que cargar bloqueos
     * @return Lista de eventos generados a partir de los bloqueos
     */
    List<Event> loadBlockagesForDate(LocalDate date);
}
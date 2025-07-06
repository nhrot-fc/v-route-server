package com.example.plgsystem.repository;

import com.example.plgsystem.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    
    /**
     * Busca incidentes por ID del vehículo
     */
    List<Incident> findByVehicleId(String vehicleId);
    
    /**
     * Busca incidentes por ID del vehículo (paginado)
     */
    Page<Incident> findByVehicleId(String vehicleId, Pageable pageable);
    
    /**
     * Busca incidentes por estado de resolución
     */
    List<Incident> findByResolved(boolean resolved);
    
    /**
     * Busca incidentes por estado de resolución (paginado)
     */
    Page<Incident> findByResolved(boolean resolved, Pageable pageable);
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas
     */
    List<Incident> findByOccurrenceTimeBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas (paginado)
     */
    Page<Incident> findByOccurrenceTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas
     */
    List<Incident> findByVehicleIdAndOccurrenceTimeBetween(String vehicleId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas (paginado)
     */
    Page<Incident> findByVehicleIdAndOccurrenceTimeBetween(String vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}

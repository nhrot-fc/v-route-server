package com.example.plgsystem.repository;

import com.example.plgsystem.model.Incident;
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
     * Busca incidentes por estado de resolución
     */
    List<Incident> findByResolved(boolean resolved);
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas
     */
    List<Incident> findByOccurrenceTimeBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas
     */
    List<Incident> findByVehicleIdAndOccurrenceTimeBetween(String vehicleId, LocalDateTime start, LocalDateTime end);
}

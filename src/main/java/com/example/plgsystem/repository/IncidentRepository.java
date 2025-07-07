package com.example.plgsystem.repository;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    
    /**
     * Busca incidentes por ID del vehículo
     */
    List<Incident> findByVehicleId(String vehicleId);
    
    /**
     * Busca incidentes por ID del vehículo (paginado)
     */
    Page<Incident> findByVehicleId(String vehicleId, Pageable pageable);

    /**
     * Busca incidentes por tipo
     */
    List<Incident> findByType(IncidentType type);

    /**
     * Busca incidentes por tipo (paginado)
     */
    Page<Incident> findByType(IncidentType type, Pageable pageable);

    /**
     * Busca incidentes por turno
     */
    List<Incident> findByShift(Shift shift);

    /**
     * Busca incidentes por turno (paginado)
     */
    Page<Incident> findByShift(Shift shift, Pageable pageable);
    
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

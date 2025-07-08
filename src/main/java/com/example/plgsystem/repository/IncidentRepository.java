package com.example.plgsystem.repository;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    @Query("SELECT i FROM Incident i ORDER BY i.occurrenceTime ASC")
    @NonNull List<Incident> findAll();

    @Query("SELECT i FROM Incident i ORDER BY i.occurrenceTime ASC")
    @NonNull Page<Incident> findAll(@NonNull Pageable pageable);

    /**
     * Busca incidentes por ID del vehículo
     */
    @Query("SELECT i FROM Incident i WHERE i.vehicle.id = :vehicleId ORDER BY i.occurrenceTime ASC")
    List<Incident> findByVehicleId(String vehicleId);
    
    /**
     * Busca incidentes por ID del vehículo (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.vehicle.id = :vehicleId ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByVehicleId(String vehicleId, Pageable pageable);

    /**
     * Busca incidentes por tipo
     */
    @Query("SELECT i FROM Incident i WHERE i.type = :type ORDER BY i.occurrenceTime ASC")
    List<Incident> findByType(IncidentType type);

    /**
     * Busca incidentes por tipo (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.type = :type ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByType(IncidentType type, Pageable pageable);

    /**
     * Busca incidentes por turno
     */
    @Query("SELECT i FROM Incident i WHERE i.shift = :shift ORDER BY i.occurrenceTime ASC")
    List<Incident> findByShift(Shift shift);

    /**
     * Busca incidentes por turno (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.shift = :shift ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByShift(Shift shift, Pageable pageable);
    
    /**
     * Busca incidentes por estado de resolución
     */
    @Query("SELECT i FROM Incident i WHERE i.resolved = :resolved ORDER BY i.occurrenceTime ASC")
    List<Incident> findByResolved(boolean resolved);
    
    /**
     * Busca incidentes por estado de resolución (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.resolved = :resolved ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByResolved(boolean resolved, Pageable pageable);
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas
     */
    @Query("SELECT i FROM Incident i WHERE i.occurrenceTime BETWEEN :start AND :end ORDER BY i.occurrenceTime ASC")
    List<Incident> findByOccurrenceTimeBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.occurrenceTime BETWEEN :start AND :end ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByOccurrenceTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas
     */
    @Query("SELECT i FROM Incident i WHERE i.vehicle.id = :vehicleId AND i.occurrenceTime BETWEEN :start AND :end ORDER BY i.occurrenceTime ASC")
    List<Incident> findByVehicleIdAndOccurrenceTimeBetween(String vehicleId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas (paginado)
     */
    @Query("SELECT i FROM Incident i WHERE i.vehicle.id = :vehicleId AND i.occurrenceTime BETWEEN :start AND :end ORDER BY i.occurrenceTime ASC")
    Page<Incident> findByVehicleIdAndOccurrenceTimeBetween(String vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}

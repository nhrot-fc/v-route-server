package com.example.plgsystem.repository;

import com.example.plgsystem.model.Maintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la entidad Maintenance
 */
@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {
    /**
     * Busca mantenimientos por ID de vehículo
     */
    List<Maintenance> findByVehicleId(String vehicleId);

    /**
     * Busca mantenimientos por ID de vehículo (paginado)
     */
    Page<Maintenance> findByVehicleId(String vehicleId, Pageable pageable);

    /**
     * Busca mantenimientos activos en una fecha específica
     */
    List<Maintenance> findByAssignedDate(LocalDate date);

    /**
     * Busca mantenimientos activos en una fecha específica (paginado)
     */
    Page<Maintenance> findByAssignedDate(LocalDate date, Pageable pageable);
    
    /**
     * Busca mantenimientos para un vehículo en una fecha específica
     */
    List<Maintenance> findByVehicleIdAndAssignedDate(String vehicleId, LocalDate date);

    /**
     * Busca mantenimientos para un vehículo en una fecha específica (paginado)
     */
    Page<Maintenance> findByVehicleIdAndAssignedDate(String vehicleId, LocalDate date, Pageable pageable);
    
    /**
     * Busca mantenimientos activos en un rango de fechas
     */
    List<Maintenance> findByAssignedDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Busca mantenimientos activos en un rango de fechas (paginado)
     */
    Page<Maintenance> findByAssignedDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Busca mantenimientos activos en el momento actual
     */
    @Query("SELECT m FROM Maintenance m WHERE m.realStart <= :currentTime AND (m.realEnd IS NULL OR m.realEnd >= :currentTime)")
    List<Maintenance> findActiveMaintenances(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Busca mantenimientos activos en el momento actual (paginado)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.realStart <= :currentTime AND (m.realEnd IS NULL OR m.realEnd >= :currentTime)")
    Page<Maintenance> findActiveMaintenancesPaged(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
}

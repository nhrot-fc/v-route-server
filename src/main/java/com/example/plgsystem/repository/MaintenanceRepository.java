package com.example.plgsystem.repository;

import com.example.plgsystem.model.Maintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
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
     * Filtro para listar todos los mantenimientos ordenados por fecha de asignación
     */
    @Query("SELECT m FROM Maintenance m ORDER BY m.assignedDate ASC")
    @NonNull List<Maintenance> findAll();

    /**
     * Filtro para listar todos los mantenimientos ordenados por fecha de asignación (paginado)
     */
    @Query("SELECT m FROM Maintenance m ORDER BY m.assignedDate ASC")
    @NonNull Page<Maintenance> findAll(@NonNull Pageable pageable);

    /**
     * Busca mantenimientos por ID de vehículo
     */
    @Query("SELECT m FROM Maintenance m WHERE m.vehicle.id = :vehicleId ORDER BY m.assignedDate ASC")
    List<Maintenance> findByVehicleId(String vehicleId);

    /**
     * Busca mantenimientos por ID de vehículo (paginado)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.vehicle.id = :vehicleId ORDER BY m.assignedDate ASC")
    Page<Maintenance> findByVehicleId(String vehicleId, Pageable pageable);

    /**
     * Busca mantenimientos activos en una fecha específica
     */
    @Query("SELECT m FROM Maintenance m WHERE m.assignedDate = :date ORDER BY m.assignedDate ASC")
    List<Maintenance> findByAssignedDate(LocalDate date);

    /**
     * Busca mantenimientos activos en una fecha específica (paginado)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.assignedDate = :date ORDER BY m.assignedDate ASC")
    Page<Maintenance> findByAssignedDate(LocalDate date, Pageable pageable);
    
    /**
     * Busca mantenimientos para un vehículo en una fecha específica
     */
    @Query("SELECT m FROM Maintenance m WHERE m.vehicle.id = :vehicleId AND m.assignedDate = :date ORDER BY m.assignedDate ASC")
    List<Maintenance> findByVehicleIdAndAssignedDate(String vehicleId, LocalDate date);

    /**
     * Busca mantenimientos para un vehículo en una fecha específica (paginado)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.vehicle.id = :vehicleId AND m.assignedDate = :date ORDER BY m.assignedDate ASC")
    Page<Maintenance> findByVehicleIdAndAssignedDate(String vehicleId, LocalDate date, Pageable pageable);
    
    /**
     * Busca mantenimientos activos en un rango de fechas
     */
    @Query("SELECT m FROM Maintenance m WHERE m.assignedDate BETWEEN :startDate AND :endDate ORDER BY m.assignedDate ASC")
    List<Maintenance> findByAssignedDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Busca mantenimientos activos en un rango de fechas (paginado)
     */
    @Query("SELECT m FROM Maintenance m WHERE m.assignedDate BETWEEN :startDate AND :endDate ORDER BY m.assignedDate ASC")
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

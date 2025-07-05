package com.example.plgsystem.repository;

import com.example.plgsystem.model.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad Maintenance
 */
@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    /**
     * Busca mantenimientos por ID de vehículo
     */
    List<Maintenance> findByVehicleId(String vehicleId);

    /**
     * Busca mantenimientos activos en una fecha específica
     */
    List<Maintenance> findByAssignedDate(LocalDate date);
    
    /**
     * Busca mantenimientos para un vehículo en una fecha específica
     */
    List<Maintenance> findByVehicleIdAndAssignedDate(String vehicleId, LocalDate date);
    
    /**
     * Busca mantenimientos activos en un rango de fechas
     */
    List<Maintenance> findByAssignedDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Busca mantenimientos activos en el momento actual
     */
    default List<Maintenance> findActiveMaintenances(LocalDateTime currentTime) {
        return this.findAll().stream()
            .filter(maintenance -> maintenance.isActiveAt(currentTime))
            .toList();
    }
}

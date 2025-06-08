package com.example.plgsystem.repository;

import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    
    // Find maintenance by vehicle
    List<Maintenance> findByVehicleId(String vehicleId);
    
    // Find maintenance by type
    List<Maintenance> findByType(MaintenanceType type);
    
    // Find active maintenance (ongoing)
    @Query("SELECT m FROM Maintenance m WHERE :currentTime BETWEEN m.startDate AND m.endDate")
    List<Maintenance> findActiveMaintenance(@Param("currentTime") LocalDateTime currentTime);
    
    // Find maintenance scheduled for a specific vehicle at a given time
    @Query("SELECT m FROM Maintenance m WHERE m.vehicleId = :vehicleId AND " +
           ":currentTime BETWEEN m.startDate AND m.endDate")
    List<Maintenance> findActiveMaintenanceForVehicle(@Param("vehicleId") String vehicleId, 
                                                     @Param("currentTime") LocalDateTime currentTime);
    
    // Find upcoming maintenance within a date range
    @Query("SELECT m FROM Maintenance m WHERE m.startDate BETWEEN :startDate AND :endDate")
    List<Maintenance> findUpcomingMaintenance(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
}

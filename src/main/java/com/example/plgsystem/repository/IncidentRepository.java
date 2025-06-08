package com.example.plgsystem.repository;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    
    // Find incidents by vehicle
    List<Incident> findByVehicleId(String vehicleId);
    
    // Find incidents by type
    List<Incident> findByType(IncidentType type);
    
    // Find active incidents (not yet resolved)
    @Query("SELECT i FROM Incident i WHERE i.resolved = false OR i.resolved IS NULL")
    List<Incident> findActiveIncidents(@Param("currentTime") LocalDateTime currentTime);
    
    // Find incidents for a specific vehicle that are still active
    @Query("SELECT i FROM Incident i WHERE i.vehicleId = :vehicleId AND (i.resolved = false OR i.resolved IS NULL)")
    List<Incident> findActiveIncidentsForVehicle(@Param("vehicleId") String vehicleId, 
                                                @Param("currentTime") LocalDateTime currentTime);
    
    // Find incidents by date range
    @Query("SELECT i FROM Incident i WHERE i.occurrenceTime BETWEEN :startDate AND :endDate")
    List<Incident> findIncidentsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // Find resolved incidents
    @Query("SELECT i FROM Incident i WHERE i.resolved = true")
    List<Incident> findResolvedIncidents(@Param("currentTime") LocalDateTime currentTime);
    
    // Find incidents by resolved status (for test compatibility)
    List<Incident> findByResolved(Boolean resolved);
}

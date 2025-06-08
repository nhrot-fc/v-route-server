package com.example.plgsystem.repository;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.VehicleStatus;
import com.example.plgsystem.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    // Find vehicles by status
    List<Vehicle> findByStatus(VehicleStatus status);
    
    // Find available vehicles
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'AVAILABLE'")
    List<Vehicle> findAvailableVehicles();
    
    // Find vehicles by type
    List<Vehicle> findByType(VehicleType type);
    
    // Find vehicles within radius
    @Query("SELECT v FROM Vehicle v WHERE " +
           "SQRT(POWER(v.currentPosition.x - :x, 2) + POWER(v.currentPosition.y - :y, 2)) <= :radius")
    List<Vehicle> findByRadius(@Param("x") double x, @Param("y") double y, @Param("radius") double radius);
    
    // Find vehicles with sufficient GLP capacity
    @Query("SELECT v FROM Vehicle v WHERE v.currentGLP >= :requiredGLP AND v.status = 'AVAILABLE'")
    List<Vehicle> findVehiclesWithSufficientGLP(@Param("requiredGLP") double requiredGLP);
    
    // Find vehicles by type and status
    List<Vehicle> findByTypeAndStatus(VehicleType type, VehicleStatus status);
    
    // Get total fleet capacity
    @Query("SELECT SUM(v.glpCapacity) FROM Vehicle v")
    Double getTotalFleetCapacity();
    
    // Get available fleet capacity
    @Query("SELECT SUM(v.currentGLP) FROM Vehicle v WHERE v.status = 'AVAILABLE'")
    Double getAvailableFleetGLP();
}

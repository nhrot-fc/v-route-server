package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepotRepository extends JpaRepository<Depot, String> {
    
    // Find depots with sufficient GLP
    @Query("SELECT d FROM Depot d WHERE d.currentGLP >= :requiredGLP")
    List<Depot> findDepotsWithSufficientGLP(@Param("requiredGLP") double requiredGLP);
    
    // Find depots by location range (useful for finding nearby depots)
    @Query("SELECT d FROM Depot d WHERE " +
           "d.position.x BETWEEN :minX AND :maxX AND " +
           "d.position.y BETWEEN :minY AND :maxY")
    List<Depot> findDepotsByLocationRange(@Param("minX") int minX, @Param("maxX") int maxX,
                                         @Param("minY") int minY, @Param("maxY") int maxY);
    
    // Get total storage capacity
    @Query("SELECT SUM(d.glpCapacity) FROM Depot d")
    Double getTotalStorageCapacity();
    
    // Get current total GLP in storage
    @Query("SELECT SUM(d.currentGLP) FROM Depot d")
    Double getCurrentTotalGLP();
}

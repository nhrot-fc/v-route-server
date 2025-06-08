package com.example.plgsystem.repository;

import com.example.plgsystem.model.Blockage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlockageRepository extends JpaRepository<Blockage, Long> {
    
    // Find active blockages at a specific time
    @Query("SELECT b FROM Blockage b WHERE :currentTime BETWEEN b.startTime AND b.endTime")
    List<Blockage> findActiveBlockages(@Param("currentTime") LocalDateTime currentTime);
    
    // Find blockages by date range
    @Query("SELECT b FROM Blockage b WHERE b.startTime <= :endDate AND b.endTime >= :startDate")
    List<Blockage> findBlockagesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // Find blockages affecting a specific route segment
    @Query("SELECT b FROM Blockage b WHERE " +
           "(b.startNode.x = :x1 AND b.startNode.y = :y1 AND b.endNode.x = :x2 AND b.endNode.y = :y2) OR " +
           "(b.startNode.x = :x2 AND b.startNode.y = :y2 AND b.endNode.x = :x1 AND b.endNode.y = :y1)")
    List<Blockage> findBlockagesForSegment(@Param("x1") int x1, @Param("y1") int y1,
                                         @Param("x2") int x2, @Param("y2") int y2);
}

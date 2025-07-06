package com.example.plgsystem.repository;

import com.example.plgsystem.model.Blockage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlockageRepository extends JpaRepository<Blockage, Long> {
    
    /**
     * Filtro para listar bloqueos activos en un momento específico
     */
    @Query("SELECT b FROM Blockage b WHERE :dateTime BETWEEN b.startTime AND b.endTime")
    List<Blockage> findByActiveAtDateTime(@Param("dateTime") LocalDateTime dateTime);
    
    /**
     * Filtro para listar bloqueos activos en un momento específico (paginado)
     */
    @Query("SELECT b FROM Blockage b WHERE :dateTime BETWEEN b.startTime AND b.endTime")
    Page<Blockage> findByActiveAtDateTime(@Param("dateTime") LocalDateTime dateTime, Pageable pageable);
    
    /**
     * Filtro para listar bloqueos por rango de tiempo
     */
    List<Blockage> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Filtro para listar bloqueos por rango de tiempo (paginado)
     */
    Page<Blockage> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
}

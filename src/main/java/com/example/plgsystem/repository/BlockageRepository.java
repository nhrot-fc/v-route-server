package com.example.plgsystem.repository;

import com.example.plgsystem.model.Blockage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockageRepository extends JpaRepository<Blockage, UUID> {
    /**
     * Filtro para listar todos los bloqueos ordenados por fecha de inicio
     */
    @Query("SELECT b FROM Blockage b ORDER BY b.startTime ASC")
    @NonNull List<Blockage> findAll();

    /**
     * Filtro para listar todos los bloqueos ordenados por fecha de inicio (paginado)
     */
    @Query("SELECT b FROM Blockage b ORDER BY b.startTime ASC")
    @NonNull Page<Blockage> findAll(@NonNull Pageable pageable);
    
    /**
     * Filtro para listar bloqueos activos en un momento específico
     */
    @Query("SELECT b FROM Blockage b WHERE :dateTime BETWEEN b.startTime AND b.endTime ORDER BY b.startTime ASC")
    List<Blockage> findByActiveAtDateTime(@Param("dateTime") LocalDateTime dateTime);
    
    /**
     * Filtro para listar bloqueos activos en un momento específico (paginado)
     */
    @Query("SELECT b FROM Blockage b WHERE :dateTime BETWEEN b.startTime AND b.endTime ORDER BY b.startTime ASC")
    Page<Blockage> findByActiveAtDateTime(@Param("dateTime") LocalDateTime dateTime, Pageable pageable);
    
    /**
     * Filtro para listar bloqueos por rango de tiempo
     */
    @Query("SELECT b FROM Blockage b WHERE b.startTime >= :startTime AND b.endTime <= :endTime ORDER BY b.startTime ASC")
    List<Blockage> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Filtro para listar bloqueos por rango de tiempo (paginado)
     */
    @Query("SELECT b FROM Blockage b WHERE b.startTime >= :startTime AND b.endTime <= :endTime ORDER BY b.startTime ASC")
    Page<Blockage> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
}

package com.example.plgsystem.repository;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    /**
     * Busca vehículos por tipo
     */
    @Query("SELECT v FROM Vehicle v WHERE v.type = :type ORDER BY v.id ASC")
    List<Vehicle> findByType(VehicleType type);
    
    /**
     * Busca vehículos por tipo (paginado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.type = :type ORDER BY v.id ASC")
    Page<Vehicle> findByType(VehicleType type, Pageable pageable);
    
    /**
     * Busca vehículos por estado
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status ORDER BY v.id ASC")
    List<Vehicle> findByStatus(VehicleStatus status);
    
    /**
     * Busca vehículos por estado (paginado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status ORDER BY v.id ASC")
    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);
    
    /**
     * Busca vehículos disponibles (por estado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status ORDER BY v.currentGlpM3 DESC")
    List<Vehicle> findByStatusOrderByCurrentGlpM3Desc(VehicleStatus status);
    
    /**
     * Busca vehículos disponibles (por estado, paginado y ordenado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status ORDER BY v.currentGlpM3 DESC")
    Page<Vehicle> findByStatusOrderByCurrentGlpM3Desc(VehicleStatus status, Pageable pageable);
    
    /**
     * Busca vehículos por capacidad de GLP restante mínima
     */
    @Query("SELECT v FROM Vehicle v WHERE v.currentGlpM3 >= :minGlp ORDER BY v.id ASC")
    List<Vehicle> findByCurrentGlpM3GreaterThanEqual(int minGlp);
    
    /**
     * Busca vehículos por capacidad de GLP restante mínima (paginado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.currentGlpM3 >= :minGlp ORDER BY v.id ASC")
    Page<Vehicle> findByCurrentGlpM3GreaterThanEqual(int minGlp, Pageable pageable);
    
    /**
     * Busca vehículos por capacidad de combustible restante mínima
     */
    @Query("SELECT v FROM Vehicle v WHERE v.currentFuelGal >= :minFuel ORDER BY v.id ASC")
    List<Vehicle> findByCurrentFuelGalGreaterThanEqual(double minFuel);
    
    /**
     * Busca vehículos por capacidad de combustible restante mínima (paginado)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.currentFuelGal >= :minFuel ORDER BY v.id ASC")
    Page<Vehicle> findByCurrentFuelGalGreaterThanEqual(double minFuel, Pageable pageable);
}

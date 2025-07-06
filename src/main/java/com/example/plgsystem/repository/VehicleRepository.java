package com.example.plgsystem.repository;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    /**
     * Busca vehículos por tipo
     */
    List<Vehicle> findByType(VehicleType type);
    
    /**
     * Busca vehículos por tipo (paginado)
     */
    Page<Vehicle> findByType(VehicleType type, Pageable pageable);
    
    /**
     * Busca vehículos por estado
     */
    List<Vehicle> findByStatus(VehicleStatus status);
    
    /**
     * Busca vehículos por estado (paginado)
     */
    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);
    
    /**
     * Busca vehículos disponibles (por estado)
     */
    List<Vehicle> findByStatusOrderByCurrentGlpM3Desc(VehicleStatus status);
    
    /**
     * Busca vehículos disponibles (por estado, paginado y ordenado)
     */
    Page<Vehicle> findByStatusOrderByCurrentGlpM3Desc(VehicleStatus status, Pageable pageable);
    
    /**
     * Busca vehículos por capacidad de GLP restante mínima
     */
    List<Vehicle> findByCurrentGlpM3GreaterThanEqual(int minGlp);
    
    /**
     * Busca vehículos por capacidad de GLP restante mínima (paginado)
     */
    Page<Vehicle> findByCurrentGlpM3GreaterThanEqual(int minGlp, Pageable pageable);
    
    /**
     * Busca vehículos por capacidad de combustible restante mínima
     */
    List<Vehicle> findByCurrentFuelGalGreaterThanEqual(double minFuel);
    
    /**
     * Busca vehículos por capacidad de combustible restante mínima (paginado)
     */
    Page<Vehicle> findByCurrentFuelGalGreaterThanEqual(double minFuel, Pageable pageable);
}

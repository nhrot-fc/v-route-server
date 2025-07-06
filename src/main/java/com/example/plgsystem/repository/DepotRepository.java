package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepotRepository extends JpaRepository<Depot, String> {

    /**
     * Filtro para listar depósitos por capacidad de recarga
     */
    List<Depot> findByCanRefuel(boolean canRefuel);
    
    /**
     * Filtro para listar depósitos por capacidad de recarga (paginado)
     */
    Page<Depot> findByCanRefuel(boolean canRefuel, Pageable pageable);
    
    /**
     * Filtro para listar depósitos por capacidad de GLP
     */
    List<Depot> findByGlpCapacityM3GreaterThanEqual(int minCapacity);
    
    /**
     * Filtro para listar depósitos por capacidad de GLP (paginado)
     */
    Page<Depot> findByGlpCapacityM3GreaterThanEqual(int minCapacity, Pageable pageable);
    
    /**
     * Filtro para listar depósitos por GLP disponible
     */
    List<Depot> findByCurrentGlpM3GreaterThanEqual(int minAmount);
    
    /**
     * Filtro para listar depósitos por GLP disponible (paginado)
     */
    Page<Depot> findByCurrentGlpM3GreaterThanEqual(int minAmount, Pageable pageable);
}

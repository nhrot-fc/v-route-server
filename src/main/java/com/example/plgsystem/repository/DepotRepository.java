package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.enums.DepotType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepotRepository extends JpaRepository<Depot, String> {

    /**
     * Filtro para listar depósitos por tipo
     */
    List<Depot> findByType(DepotType type);
    
    /**
     * Filtro para listar depósitos por tipo (paginado)
     */
    Page<Depot> findByType(DepotType type, Pageable pageable);
    
    /**
     * Filtro para listar depósitos principales
     */
    default List<Depot> findMainDepots() {
        return findByType(DepotType.MAIN);
    }
    
    /**
     * Filtro para listar depósitos auxiliares
     */
    default List<Depot> findAuxiliaryDepots() {
        return findByType(DepotType.AUXILIARY);
    }
    
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

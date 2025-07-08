package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.enums.DepotType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.lang.NonNull;

@Repository
public interface DepotRepository extends JpaRepository<Depot, String> {
    @Query("SELECT d FROM Depot d ORDER BY d.id ASC")
    @NonNull
    List<Depot> findAll();

    @Query("SELECT d FROM Depot d ORDER BY d.id ASC")
    @NonNull
    Page<Depot> findAll(@NonNull Pageable pageable);

    /**
     * Filtro para listar depósitos por tipo
     */
    @Query("SELECT d FROM Depot d WHERE d.type = :type ORDER BY d.id ASC")
    List<Depot> findByType(DepotType type);

    /**
     * Filtro para listar depósitos por tipo (paginado)
     */
    @Query("SELECT d FROM Depot d WHERE d.type = :type ORDER BY d.id ASC")
    Page<Depot> findByType(DepotType type, Pageable pageable);

    /**
     * Filtro para listar depósitos principales
     */
    @Query("SELECT d FROM Depot d WHERE d.type = com.example.plgsystem.enums.DepotType.MAIN ORDER BY d.id ASC")
    default List<Depot> findMainDepots() {
        return findByType(DepotType.MAIN);
    }

    /**
     * Filtro para listar depósitos auxiliares
     */
    @Query("SELECT d FROM Depot d WHERE d.type = com.example.plgsystem.enums.DepotType.AUXILIARY ORDER BY d.id ASC")
    default List<Depot> findAuxiliaryDepots() {
        return findByType(DepotType.AUXILIARY);
    }

    /**
     * Filtro para listar depósitos por capacidad de GLP
     */
    @Query("SELECT d FROM Depot d WHERE d.glpCapacityM3 >= :minCapacity ORDER BY d.id ASC")
    List<Depot> findByGlpCapacityM3GreaterThanEqual(int minCapacity);

    /**
     * Filtro para listar depósitos por capacidad de GLP (paginado)
     */
    @Query("SELECT d FROM Depot d WHERE d.glpCapacityM3 >= :minCapacity ORDER BY d.id ASC")
    Page<Depot> findByGlpCapacityM3GreaterThanEqual(int minCapacity, Pageable pageable);

    /**
     * Filtro para listar depósitos por GLP disponible
     */
    @Query("SELECT d FROM Depot d WHERE d.currentGlpM3 >= :minAmount ORDER BY d.id ASC")
    List<Depot> findByCurrentGlpM3GreaterThanEqual(int minAmount);

    /**
     * Filtro para listar depósitos por GLP disponible (paginado)
     */
    @Query("SELECT d FROM Depot d WHERE d.currentGlpM3 >= :minAmount ORDER BY d.id ASC")
    Page<Depot> findByCurrentGlpM3GreaterThanEqual(int minAmount, Pageable pageable);
}

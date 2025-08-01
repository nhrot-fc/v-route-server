package com.example.plgsystem.repository;

import com.example.plgsystem.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    /**
     * Filtro para verificar si existen pedidos por ID
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.id IN :ids")
    boolean existsByIdIn(@Param("ids") List<String> ids);

    /**
     * Filtro para listar todos los pedidos ordenados por fecha de llegada
     */
    @Query("SELECT o FROM Order o ORDER BY o.arrivalTime ASC")
    @NonNull List<Order> findAll();

    /**
     * Filtro para listar todos los pedidos ordenados por fecha de llegada (paginado)
     */
    @Query("SELECT o FROM Order o ORDER BY o.arrivalTime ASC")
    @NonNull Page<Order> findAll(@NonNull Pageable pageable);

    /**
     * Filtro para listar pedidos por estado de entrega
     */
    @Query("SELECT o FROM Order o WHERE o.remainingGlpM3 = :remainingGlp ORDER BY o.arrivalTime ASC")
    List<Order> findByRemainingGlpM3(int remainingGlp);
    
    /**
     * Filtro para listar pedidos con entrega pendiente
     */
    @Query("SELECT o FROM Order o WHERE o.remainingGlpM3 > 0 ORDER BY o.arrivalTime ASC")
    List<Order> findPendingDeliveries();
    
    /**
     * Filtro para listar pedidos con entrega pendiente (paginado)
     */
    @Query("SELECT o FROM Order o WHERE o.remainingGlpM3 > 0 ORDER BY o.arrivalTime ASC")
    Page<Order> findPendingDeliveriesPaged(Pageable pageable);
    
    /**
     * Filtro para listar pedidos con fecha límite anterior a una fecha dada
     */
    @Query("SELECT o FROM Order o WHERE o.deadlineTime < :dateTime ORDER BY o.arrivalTime ASC")
    List<Order> findByDeadlineTimeBefore(LocalDateTime dateTime);
    
    /**
     * Filtro para listar pedidos con fecha límite anterior a una fecha dada (paginado)
     */
    @Query("SELECT o FROM Order o WHERE o.deadlineTime < :dateTime ORDER BY o.arrivalTime ASC")
    Page<Order> findByDeadlineTimeBefore(LocalDateTime dateTime, Pageable pageable);
    
    /**
     * Filtro para listar pedidos disponibles para entrega (llegada antes de fecha actual)
     */
    @Query("SELECT o FROM Order o WHERE o.arrivalTime <= :dateTime ORDER BY o.arrivalTime ASC")
    List<Order> findByArrivalTimeLessThanEqual(LocalDateTime dateTime);
    
    /**
     * Filtro para listar pedidos disponibles para entrega (llegada antes de fecha actual) (paginado)
     */
    @Query("SELECT o FROM Order o WHERE o.arrivalTime <= :dateTime ORDER BY o.arrivalTime ASC")
    Page<Order> findByArrivalTimeLessThanEqual(LocalDateTime dateTime, Pageable pageable);

    /**
     * Busca órdenes con hora de llegada entre dos fechas
     * 
     * @param start Fecha y hora de inicio
     * @param end Fecha y hora de fin
     * @return Lista de órdenes que llegan en el periodo especificado
     */
    @Query("SELECT o FROM Order o WHERE o.remainingGlpM3 > 0 AND o.arrivalTime BETWEEN :start AND :end ORDER BY o.arrivalTime ASC")
    List<Order> findPendingByArrivalTimeBetween(LocalDateTime start, LocalDateTime end);
}

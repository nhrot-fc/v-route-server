package com.example.plgsystem.repository;

import com.example.plgsystem.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    /**
     * Filtro para listar pedidos por estado de entrega
     */
    List<Order> findByRemainingGlpM3(int remainingGlp);
    
    /**
     * Filtro para listar pedidos con entrega pendiente
     */
    @Query("SELECT o FROM Order o WHERE o.remainingGlpM3 > 0")
    List<Order> findPendingDeliveries();
    
    /**
     * Filtro para listar pedidos con fecha límite anterior a una fecha dada
     */
    List<Order> findByDueTimeBefore(LocalDateTime dateTime);
    
    /**
     * Filtro para listar pedidos disponibles para entrega (llegada antes de fecha actual)
     */
    List<Order> findByArriveTimeLessThanEqual(LocalDateTime dateTime);
}

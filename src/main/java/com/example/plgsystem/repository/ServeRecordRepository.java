package com.example.plgsystem.repository;

import com.example.plgsystem.model.ServeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServeRecordRepository extends JpaRepository<ServeRecord, UUID> {
    
    /**
     * Busca registros de entrega por ID de pedido
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.order.id = :orderId ORDER BY s.serveDate ASC")
    List<ServeRecord> findByOrderId(String orderId);
    
    /**
     * Busca registros de entrega por ID de pedido (paginado)
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.order.id = :orderId ORDER BY s.serveDate ASC")
    Page<ServeRecord> findByOrderId(String orderId, Pageable pageable);
    
    /**
     * Busca registros de entrega por ID de vehículo
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.vehicle.id = :vehicleId ORDER BY s.serveDate ASC")
    List<ServeRecord> findByVehicleId(String vehicleId);
    
    /**
     * Busca registros de entrega por ID de vehículo (paginado)
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.vehicle.id = :vehicleId ORDER BY s.serveDate ASC")
    Page<ServeRecord> findByVehicleId(String vehicleId, Pageable pageable);
    
    /**
     * Busca registros de entrega por rango de fechas
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.serveDate BETWEEN :start AND :end ORDER BY s.serveDate ASC")
    List<ServeRecord> findByServeDateBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca registros de entrega por rango de fechas (paginado)
     */
    @Query("SELECT s FROM ServeRecord s WHERE s.serveDate BETWEEN :start AND :end ORDER BY s.serveDate ASC")
    Page<ServeRecord> findByServeDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}

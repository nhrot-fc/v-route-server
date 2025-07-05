package com.example.plgsystem.repository;

import com.example.plgsystem.model.ServeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServeRecordRepository extends JpaRepository<ServeRecord, Long> {
    
    /**
     * Busca registros de entrega por ID de pedido
     */
    List<ServeRecord> findByOrderId(String orderId);
    
    /**
     * Busca registros de entrega por ID de vehículo
     */
    List<ServeRecord> findByVehicleId(String vehicleId);
    
    /**
     * Busca registros de entrega por rango de fechas
     */
    List<ServeRecord> findByServeDateBetween(LocalDateTime start, LocalDateTime end);
}

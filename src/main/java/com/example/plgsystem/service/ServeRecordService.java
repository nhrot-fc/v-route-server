package com.example.plgsystem.service;

import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.repository.ServeRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServeRecordService {

    private final ServeRecordRepository serveRecordRepository;

    public ServeRecordService(ServeRecordRepository serveRecordRepository) {
        this.serveRecordRepository = serveRecordRepository;
    }

    /**
     * Guarda un registro de entrega en la base de datos
     */
    @Transactional
    public ServeRecord save(ServeRecord serveRecord) {
        return serveRecordRepository.save(serveRecord);
    }

    /**
     * Busca un registro de entrega por su ID
     */
    public Optional<ServeRecord> findById(UUID id) {
        return serveRecordRepository.findById(id);
    }

    /**
     * Obtiene todos los registros de entrega
     */
    public List<ServeRecord> findAll() {
        return serveRecordRepository.findAll();
    }
    
    /**
     * Obtiene todos los registros de entrega (paginado)
     */
    public Page<ServeRecord> findAllPaged(Pageable pageable) {
        return serveRecordRepository.findAll(pageable);
    }

    /**
     * Elimina un registro de entrega por ID
     */
    @Transactional
    public void deleteById(UUID id) {
        serveRecordRepository.deleteById(id);
    }
    
    /**
     * Busca registros de entrega por ID de pedido
     */
    public List<ServeRecord> findByOrderId(String orderId) {
        return serveRecordRepository.findByOrderId(orderId);
    }
    
    /**
     * Busca registros de entrega por ID de pedido (paginado)
     */
    public Page<ServeRecord> findByOrderIdPaged(String orderId, Pageable pageable) {
        return serveRecordRepository.findByOrderId(orderId, pageable);
    }
    
    /**
     * Busca registros de entrega por ID de vehículo
     */
    public List<ServeRecord> findByVehicleId(String vehicleId) {
        return serveRecordRepository.findByVehicleId(vehicleId);
    }
    
    /**
     * Busca registros de entrega por ID de vehículo (paginado)
     */
    public Page<ServeRecord> findByVehicleIdPaged(String vehicleId, Pageable pageable) {
        return serveRecordRepository.findByVehicleId(vehicleId, pageable);
    }
    
    /**
     * Busca registros de entrega por rango de fechas
     */
    public List<ServeRecord> findByServeDateBetween(LocalDateTime start, LocalDateTime end) {
        return serveRecordRepository.findByServeDateBetween(start, end);
    }
    
    /**
     * Busca registros de entrega por rango de fechas (paginado)
     */
    public Page<ServeRecord> findByServeDateBetweenPaged(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return serveRecordRepository.findByServeDateBetween(start, end, pageable);
    }
}

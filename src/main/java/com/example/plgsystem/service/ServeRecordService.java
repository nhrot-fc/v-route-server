package com.example.plgsystem.service;

import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.repository.ServeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    public Optional<ServeRecord> findById(Long id) {
        return serveRecordRepository.findById(id);
    }

    /**
     * Obtiene todos los registros de entrega
     */
    public List<ServeRecord> findAll() {
        return serveRecordRepository.findAll();
    }

    /**
     * Elimina un registro de entrega por ID
     */
    @Transactional
    public void deleteById(Long id) {
        serveRecordRepository.deleteById(id);
    }
    
    /**
     * Busca registros de entrega por ID de pedido
     */
    public List<ServeRecord> findByOrderId(String orderId) {
        return serveRecordRepository.findByOrderId(orderId);
    }
    
    /**
     * Busca registros de entrega por ID de vehículo
     */
    public List<ServeRecord> findByVehicleId(String vehicleId) {
        return serveRecordRepository.findByVehicleId(vehicleId);
    }
    
    /**
     * Busca registros de entrega por rango de fechas
     */
    public List<ServeRecord> findByServeDateBetween(LocalDateTime start, LocalDateTime end) {
        return serveRecordRepository.findByServeDateBetween(start, end);
    }
}

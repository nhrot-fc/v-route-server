package com.example.plgsystem.service;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.repository.OrderRepository;
import com.example.plgsystem.repository.VehicleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    public OrderService(OrderRepository orderRepository, VehicleRepository vehicleRepository) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Guarda un pedido en la base de datos (crear o actualizar)
     */
    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }
    
    @Transactional
    public List<Order> saveAll(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }

    /**
     * Saves a list of orders in a separate background thread.
     * This method will return immediately.
     */
    @Async // <-- This tells Spring to run this in a background thread pool
    @Transactional
    public void saveAllAsync(List<Order> orders) {
        logger.info("⏳ Starting async bulk save of {} orders.", orders.size());
        try {
            // The batching logic still applies here automatically!
            orderRepository.saveAll(orders);
            logger.info("✅ Async bulk save completed successfully.");
        } catch (Exception e) {
            // You must handle errors here, as they won't propagate to the controller
            logger.error("❌ Error during async bulk save", e);
        }
    }

    /**
     * Busca un pedido por su ID
     */
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    public boolean existsByIdIn(List<String> ids) {
        return orderRepository.existsByIdIn(ids);
    }

    /**
     * Obtiene todos los pedidos
     */
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /**
     * Obtiene todos los pedidos con paginación
     */
    public Page<Order> findAllPaged(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * Elimina un pedido por ID
     */
    @Transactional
    public void deleteById(String id) {
        orderRepository.deleteById(id);
    }
    
    /**
     * Obtiene pedidos pendientes de entrega
     */
    public List<Order> findPendingDeliveries() {
        return orderRepository.findPendingDeliveries();
    }
    
    /**
     * Obtiene pedidos pendientes de entrega con paginación
     */
    public Page<Order> findPendingDeliveriesPaged(Pageable pageable) {
        return orderRepository.findPendingDeliveriesPaged(pageable);
    }
    
    /**
     * Obtiene pedidos vencidos a una fecha dada
     */
    public List<Order> findOverdueOrders(LocalDateTime dateTime) {
        return orderRepository.findByDeadlineTimeBefore(dateTime);
    }
    
    /**
     * Obtiene pedidos vencidos a una fecha dada con paginación
     */
    public Page<Order> findOverdueOrdersPaged(LocalDateTime dateTime, Pageable pageable) {
        return orderRepository.findByDeadlineTimeBefore(dateTime, pageable);
    }
    
    /**
     * Obtiene pedidos disponibles para entrega
     */
    public List<Order> findAvailableOrders(LocalDateTime dateTime) {
        return orderRepository.findByArrivalTimeLessThanEqual(dateTime);
    }
    
    /**
     * Obtiene pedidos disponibles para entrega con paginación
     */
    public Page<Order> findAvailableOrdersPaged(LocalDateTime dateTime, Pageable pageable) {
        return orderRepository.findByArrivalTimeLessThanEqual(dateTime, pageable);
    }
    
    /**
     * Registra entrega parcial o total de un pedido
     */
    @Transactional
    public Optional<ServeRecord> recordDelivery(String orderId, int deliveredVolumeM3, String vehicleId, LocalDateTime serveDate) {
        Optional<Order> orderOpt = findById(orderId);
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        
        if (orderOpt.isPresent() && vehicleOpt.isPresent()) {
            Order order = orderOpt.get();
            Vehicle vehicle = vehicleOpt.get();
            
            ServeRecord record = vehicle.serveOrder(order, deliveredVolumeM3, serveDate);
            orderRepository.save(order);
            return Optional.of(record);
        }
        
        return Optional.empty();
    }
}

package com.example.plgsystem.service;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Guarda un pedido en la base de datos (crear o actualizar)
     */
    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Busca un pedido por su ID
     */
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
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
        return orderRepository.findByDueTimeBefore(dateTime);
    }
    
    /**
     * Obtiene pedidos vencidos a una fecha dada con paginación
     */
    public Page<Order> findOverdueOrdersPaged(LocalDateTime dateTime, Pageable pageable) {
        return orderRepository.findByDueTimeBefore(dateTime, pageable);
    }
    
    /**
     * Obtiene pedidos disponibles para entrega
     */
    public List<Order> findAvailableOrders(LocalDateTime dateTime) {
        return orderRepository.findByArriveTimeLessThanEqual(dateTime);
    }
    
    /**
     * Obtiene pedidos disponibles para entrega con paginación
     */
    public Page<Order> findAvailableOrdersPaged(LocalDateTime dateTime, Pageable pageable) {
        return orderRepository.findByArriveTimeLessThanEqual(dateTime, pageable);
    }
    
    /**
     * Registra entrega parcial o total de un pedido
     */
    @Transactional
    public Optional<ServeRecord> recordDelivery(String orderId, int deliveredVolumeM3, String vehicleId, LocalDateTime serveDate) {
        return findById(orderId)
                .map(order -> {
                    ServeRecord record = order.recordDelivery(deliveredVolumeM3, vehicleId, serveDate);
                    orderRepository.save(order);
                    return record;
                });
    }
}

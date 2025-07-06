package com.example.plgsystem.controller;

import com.example.plgsystem.dto.DeliveryRecordDTO;
import com.example.plgsystem.dto.OrderDTO;
import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.ServeRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ServeRecordService serveRecordService;

    public OrderController(OrderService orderService, ServeRecordService serveRecordService) {
        this.orderService = orderService;
        this.serveRecordService = serveRecordService;
    }

    /**
     * Crear un nuevo pedido
     */
    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO orderDTO) {
        if (orderDTO.getId() == null || orderDTO.getId().isEmpty()) {
            // Generate a unique ID if not provided
            orderDTO.setId(java.util.UUID.randomUUID().toString());
        }
        Order order = orderDTO.toEntity();
        Order savedOrder = orderService.save(order);
        return new ResponseEntity<>(OrderDTO.fromEntity(savedOrder), HttpStatus.CREATED);
    }

    /**
     * Actualizar un pedido existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> update(@PathVariable String id, @RequestBody OrderDTO orderDTO) {
        return orderService.findById(id)
                .map(existingOrder -> {
                    Order order = orderDTO.toEntity();
                    // En lugar de usar setId, construimos un nuevo objeto con el ID correcto
                    Order orderWithCorrectId = Order.builder()
                            .id(id)
                            .arriveTime(order.getArriveTime())
                            .dueTime(order.getDueTime())
                            .glpRequestM3(order.getGlpRequestM3())
                            .position(order.getPosition())
                            .build();
                    orderWithCorrectId.setRemainingGlpM3(order.getRemainingGlpM3());
                    Order updatedOrder = orderService.save(orderWithCorrectId);
                    return ResponseEntity.ok(OrderDTO.fromEntity(updatedOrder));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Obtener un pedido por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable String id) {
        Optional<Order> order = orderService.findById(id);
        return order.map(o -> ResponseEntity.ok(OrderDTO.fromEntity(o)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Listar todos los pedidos con opciones de filtrado
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> list(
            @RequestParam(required = false) Boolean pending,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime overdueAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableAt) {
        
        List<Order> orders;
        
        if (Boolean.TRUE.equals(pending)) {
            // Filtrar por pedidos pendientes
            orders = orderService.findPendingDeliveries();
        } else if (overdueAt != null) {
            // Filtrar por pedidos vencidos
            orders = orderService.findOverdueOrders(overdueAt);
        } else if (availableAt != null) {
            // Filtrar por pedidos disponibles
            orders = orderService.findAvailableOrders(availableAt);
        } else {
            // Sin filtros, retornar todos
            orders = orderService.findAll();
        }
        
        List<OrderDTO> orderDTOs = orders.stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(orderDTOs);
    }

    /**
     * Eliminar un pedido por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return orderService.findById(id)
                .map(order -> {
                    orderService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Registrar entrega de un pedido
     */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<ServeRecordDTO> recordDelivery(
            @PathVariable String id,
            @RequestBody DeliveryRecordDTO deliveryRecord) {
        
        LocalDateTime deliveryTime = deliveryRecord.getServeDate() != null ? 
                deliveryRecord.getServeDate() : LocalDateTime.now();
        
        return orderService.recordDelivery(
                id, 
                deliveryRecord.getVolumeM3(), 
                deliveryRecord.getVehicleId(), 
                deliveryTime)
                .map(serveRecord -> {
                    ServeRecord savedRecord = serveRecordService.save(serveRecord);
                    return ResponseEntity.ok(ServeRecordDTO.fromEntity(savedRecord));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.example.plgsystem.controller;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.OrderStatus;
import com.example.plgsystem.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Gestión de órdenes de entrega del sistema PLG")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Operation(summary = "Obtener todas las órdenes", description = "Retorna la lista completa de órdenes registradas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Order.class)) })
    })
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Operation(summary = "Obtener orden por ID", description = "Retorna una orden específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orden encontrada",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Order.class)) }),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "ID de la orden") @PathVariable String id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener órdenes pendientes", description = "Retorna todas las órdenes que están pendientes de entrega")
    @GetMapping("/pending")
    public List<Order> getPendingOrders() {
        return orderRepository.findPendingOrders();
    }

    @Operation(summary = "Obtener órdenes completadas", description = "Retorna todas las órdenes que han sido completadas")
    @GetMapping("/completed")
    public List<Order> getCompletedOrders() {
        return orderRepository.findCompletedOrders();
    }

    @Operation(summary = "Obtener órdenes vencidas", description = "Retorna todas las órdenes que han superado su fecha límite")
    @GetMapping("/overdue")
    public List<Order> getOverdueOrders() {
        return orderRepository.findOverdueOrders(LocalDateTime.now());
    }

    @Operation(summary = "Obtener órdenes urgentes", description = "Retorna órdenes que deben ser entregadas en las próximas horas")
    @GetMapping("/urgent")
    public List<Order> getUrgentOrders(
            @Parameter(description = "Horas hacia adelante para considerar urgente") 
            @RequestParam(defaultValue = "4") int hoursAhead) {
        LocalDateTime deadline = LocalDateTime.now().plusHours(hoursAhead);
        return orderRepository.findUrgentOrders(deadline);
    }

    @Operation(summary = "Crear nueva orden", description = "Registra una nueva orden en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orden creada exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Order.class)) })
    })
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderRepository.save(order);
    }

    @Operation(summary = "Registrar entrega", description = "Registra la entrega de una cantidad específica de GLP para una orden")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entrega registrada exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Order.class)) }),
        @ApiResponse(responseCode = "400", description = "Cantidad de entrega inválida"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PutMapping("/{id}/deliver")
    public ResponseEntity<Order> recordDelivery(
            @Parameter(description = "ID de la orden") @PathVariable String id, 
            @Parameter(description = "Volumen entregado") @RequestParam("amount") double deliveredVolume) {
        Optional<Order> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            
            // Check if trying to deliver more than remaining
            if (deliveredVolume > order.getRemainingGLP()) {
                return ResponseEntity.badRequest().build();
            }
            
            order.recordDelivery(deliveredVolume, LocalDateTime.now());
            Order updatedOrder = orderRepository.save(order);
            return ResponseEntity.ok(updatedOrder);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Obtener órdenes por rango de fechas", description = "Retorna órdenes en un rango de fechas específico")
    @GetMapping("/date-range")
    public List<Order> getOrdersByDateRange(
            @Parameter(description = "Fecha de inicio (ISO 8601)") @RequestParam String startDate, 
            @Parameter(description = "Fecha de fin (ISO 8601)") @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return orderRepository.findOrdersByArrivalDateRange(start, end);
    }

    @Operation(summary = "Obtener órdenes por fecha límite", description = "Retorna órdenes que deben ser entregadas antes de una fecha específica")
    @GetMapping("/due-by")
    public List<Order> getOrdersByDueTime(
            @Parameter(description = "Fecha límite (ISO 8601)") @RequestParam String time) {
        LocalDateTime dueTime = LocalDateTime.parse(time);
        return orderRepository.findOrdersByDueDate(LocalDateTime.now(), dueTime);
    }

    @Operation(summary = "Obtener órdenes por radio", description = "Retorna órdenes dentro de un radio específico desde una posición")
    @GetMapping("/radius")
    public List<Order> getOrdersByRadius(
            @Parameter(description = "Coordenada X del centro") @RequestParam int x, 
            @Parameter(description = "Coordenada Y del centro") @RequestParam int y, 
            @Parameter(description = "Radio de búsqueda") @RequestParam double radius) {
        return orderRepository.findOrdersByRadius(x, y, radius);
    }

    @Operation(summary = "Eliminar orden", description = "Elimina una orden del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orden eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "ID de la orden a eliminar") @PathVariable String id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Obtener órdenes por estado", description = "Retorna todas las órdenes con un estado específico (PENDING, COMPLETED, OVERDUE, etc.)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Órdenes obtenidas exitosamente",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "400", description = "Estado inválido")
    })
    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(
            @Parameter(description = "Estado de la orden (ej: PENDING, COMPLETED, OVERDUE)", example = "PENDING")
            @PathVariable("status") OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
}

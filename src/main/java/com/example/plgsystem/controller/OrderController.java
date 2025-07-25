package com.example.plgsystem.controller;

import com.example.plgsystem.dto.DeliveryRecordDTO;
import com.example.plgsystem.dto.OrderDTO;
import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        logger.info("OrderController initialized");
    }

    /**
     * Crear un nuevo pedido
     */
    @PostMapping
    public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO orderDTO) {
        logger.info("Creating new order: {}", orderDTO);
        if (orderDTO.getId() == null || orderDTO.getId().isEmpty()) {
            // Generate a unique ID if not provided
            orderDTO.setId(java.util.UUID.randomUUID().toString());
            logger.info("Generated new UUID for order: {}", orderDTO.getId());
        }
        Order order = orderDTO.toEntity();
        Order savedOrder = orderService.save(order);
        logger.info("Order created with ID: {}", savedOrder.getId());
        return new ResponseEntity<>(OrderDTO.fromEntity(savedOrder), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> createBulk(@RequestBody List<OrderDTO> orderDTOs) {
        logger.info("Received request to create {} orders in bulk", orderDTOs.size());

        // You can still perform quick validations synchronously
        List<String> orderIds = orderDTOs.stream().map(OrderDTO::getId).collect(Collectors.toList());
        if (orderService.existsByIdIn(orderIds)) {
            logger.warn("Bulk order creation rejected - one or more orders already exist.");
            return ResponseEntity.badRequest().body("One or more orders already exist.");
        }

        List<Order> ordersToSave = orderDTOs.stream()
                .map(OrderDTO::toEntity)
                .toList();

        // Call the ASYNC method
        orderService.saveAllAsync(ordersToSave);

        // Immediately return a 202 Accepted response
        logger.info("Request accepted. Handing off to async processor.");
        String responseMessage = "Bulk order request accepted. The " + ordersToSave.size()
                + " orders are being processed in the background.";
        return ResponseEntity.accepted().body(responseMessage);
    }

    /**
     * Actualizar un pedido existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> update(@PathVariable String id, @RequestBody OrderDTO orderDTO) {
        logger.info("Updating order with ID: {}", id);
        return orderService.findById(id)
                .map(existingOrder -> {
                    Order order = orderDTO.toEntity();
                    // En lugar de usar setId, construimos un nuevo objeto con el ID correcto
                    Order orderWithCorrectId = new Order(id,
                            order.getArrivalTime(),
                            order.getDeadlineTime(),
                            order.getGlpRequestM3(),
                            order.getPosition());
                    orderWithCorrectId.setRemainingGlpM3(order.getRemainingGlpM3());
                    Order updatedOrder = orderService.save(orderWithCorrectId);
                    logger.info("Order with ID: {} was updated successfully", id);
                    return ResponseEntity.ok(OrderDTO.fromEntity(updatedOrder));
                })
                .orElseGet(() -> {
                    logger.warn("Order with ID: {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Obtener un pedido por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable String id) {
        logger.info("Fetching order with ID: {}", id);
        Optional<Order> order = orderService.findById(id);
        if (order.isPresent()) {
            logger.info("Order found with ID: {}", id);
            return ResponseEntity.ok(order.get());
        } else {
            logger.warn("Order with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los pedidos con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Boolean pending,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime overdueAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableAt,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        logger.info(
                "Listing orders with filters - pending: {}, overdueAt: {}, availableAt: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                pending, overdueAt, availableAt, paginated, page, size, sortBy, direction);

        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Order> orders;

            if (Boolean.TRUE.equals(pending)) {
                // Filtrar por pedidos pendientes
                logger.info("Filtering orders by pending status");
                orders = orderService.findPendingDeliveries();
            } else if (overdueAt != null) {
                // Filtrar por pedidos vencidos
                logger.info("Filtering orders by overdue status at: {}", overdueAt);
                orders = orderService.findOverdueOrders(overdueAt);
            } else if (availableAt != null) {
                // Filtrar por pedidos disponibles
                logger.info("Filtering orders by available status at: {}", availableAt);
                orders = orderService.findAvailableOrders(availableAt);
            } else {
                // Sin filtros, retornar todos
                logger.info("Retrieving all orders without filtering");
                orders = orderService.findAll();
            }

            List<OrderDTO> orderDTOs = orders.stream()
                    .map(OrderDTO::fromEntity)
                    .collect(Collectors.toList());

            logger.info("Found {} orders matching criteria", orderDTOs.size());
            return ResponseEntity.ok(orderDTOs);
        }

        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orderPage;

        if (Boolean.TRUE.equals(pending)) {
            // Filtrar por pedidos pendientes
            logger.info("Filtering paginated orders by pending status");
            orderPage = orderService.findPendingDeliveriesPaged(pageable);
        } else if (overdueAt != null) {
            // Filtrar por pedidos vencidos
            logger.info("Filtering paginated orders by overdue status at: {}", overdueAt);
            orderPage = orderService.findOverdueOrdersPaged(overdueAt, pageable);
        } else if (availableAt != null) {
            // Filtrar por pedidos disponibles
            logger.info("Filtering paginated orders by available status at: {}", availableAt);
            orderPage = orderService.findAvailableOrdersPaged(availableAt, pageable);
        } else {
            // Sin filtros, retornar todos
            logger.info("Retrieving all paginated orders without filtering");
            orderPage = orderService.findAllPaged(pageable);
        }

        logger.info("Found page {} of {} with {} orders per page (total: {})",
                orderPage.getNumber(), orderPage.getTotalPages(), orderPage.getSize(), orderPage.getTotalElements());
        return ResponseEntity.ok(orderPage.map(OrderDTO::fromEntity));
    }

    /**
     * Eliminar un pedido por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("Attempting to delete order with ID: {}", id);
        return orderService.findById(id)
                .map(order -> {
                    orderService.deleteById(id);
                    logger.info("Order with ID: {} was deleted successfully", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    logger.warn("Order with ID: {} not found for deletion", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Registrar entrega de un pedido
     */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<ServeRecordDTO> recordDelivery(
            @PathVariable String id,
            @RequestBody DeliveryRecordDTO deliveryRecord) {

        logger.info("Recording delivery for order ID: {}, vehicle ID: {}, volume: {} m3",
                id, deliveryRecord.getVehicleId(), deliveryRecord.getVolumeM3());

        LocalDateTime deliveryTime = deliveryRecord.getServeDate() != null ? deliveryRecord.getServeDate()
                : LocalDateTime.now();

        return orderService.recordDelivery(
                id,
                deliveryRecord.getVolumeM3(),
                deliveryRecord.getVehicleId(),
                deliveryTime)
                .map(serveRecord -> {
                    ServeRecordDTO dto = new ServeRecordDTO();
                    dto.setId(serveRecord.getId());
                    dto.setVehicleId(serveRecord.getVehicle().getId());
                    dto.setOrderId(serveRecord.getOrder().getId());
                    dto.setGlpVolumeM3(serveRecord.getGlpVolumeM3());
                    dto.setServeDate(serveRecord.getServeDate());
                    logger.info("Delivery recorded successfully for order ID: {}, serve record ID: {}",
                            id, serveRecord.getId());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Failed to record delivery for order ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}

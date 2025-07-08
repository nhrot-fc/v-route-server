package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.ServeRecordService;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.VehicleService;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/serve-records")
public class ServeRecordController {

    private static final Logger logger = LoggerFactory.getLogger(ServeRecordController.class);
    private final ServeRecordService serveRecordService;
    private final VehicleService vehicleService;
    private final OrderService orderService;

    public ServeRecordController(ServeRecordService serveRecordService, VehicleService vehicleService, OrderService orderService) {
        this.serveRecordService = serveRecordService;
        this.vehicleService = vehicleService;
        this.orderService = orderService;
        logger.info("ServeRecordController initialized");
    }

    /**
     * Obtener un registro de entrega por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServeRecordDTO> getById(@PathVariable UUID id) {
        logger.info("Fetching serve record with ID: {}", id);
        Optional<ServeRecord> record = serveRecordService.findById(id);
        if (record.isPresent()) {
            logger.info("Serve record found with ID: {}", id);
            return ResponseEntity.ok(ServeRecordDTO.fromEntity(record.get()));
        } else {
            logger.warn("Serve record with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los registros de entrega con opciones de filtrado y paginación
     * opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "serveDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        logger.info("Listing serve records with filters - orderId: {}, vehicleId: {}, startDate: {}, endDate: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                orderId, vehicleId, startDate, endDate, paginated, page, size, sortBy, direction);

        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<ServeRecord> records;

            if (orderId != null) {
                logger.info("Filtering serve records by order ID: {}", orderId);
                records = serveRecordService.findByOrderId(orderId);
            } else if (vehicleId != null) {
                logger.info("Filtering serve records by vehicle ID: {}", vehicleId);
                records = serveRecordService.findByVehicleId(vehicleId);
            } else if (startDate != null && endDate != null) {
                logger.info("Filtering serve records by date range: {} to {}", startDate, endDate);
                records = serveRecordService.findByServeDateBetween(startDate, endDate);
            } else {
                logger.info("Retrieving all serve records without filtering");
                records = serveRecordService.findAll();
            }

            List<ServeRecordDTO> recordDTOs = records.stream()
                    .map(ServeRecordDTO::fromEntity)
                    .collect(Collectors.toList());

            logger.info("Found {} serve records matching criteria", recordDTOs.size());
            return ResponseEntity.ok(recordDTOs);
        }

        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ServeRecord> recordsPage;

        if (orderId != null) {
            logger.info("Filtering paginated serve records by order ID: {}", orderId);
            recordsPage = serveRecordService.findByOrderIdPaged(orderId, pageable);
        } else if (vehicleId != null) {
            logger.info("Filtering paginated serve records by vehicle ID: {}", vehicleId);
            recordsPage = serveRecordService.findByVehicleIdPaged(vehicleId, pageable);
        } else if (startDate != null && endDate != null) {
            logger.info("Filtering paginated serve records by date range: {} to {}", startDate, endDate);
            recordsPage = serveRecordService.findByServeDateBetweenPaged(startDate, endDate, pageable);
        } else {
            logger.info("Retrieving all paginated serve records without filtering");
            recordsPage = serveRecordService.findAllPaged(pageable);
        }

        logger.info("Found page {} of {} with {} serve records per page (total: {})", 
                recordsPage.getNumber(), recordsPage.getTotalPages(), recordsPage.getSize(), recordsPage.getTotalElements());
        return ResponseEntity.ok(recordsPage.map(ServeRecordDTO::fromEntity));
    }
    
    /**
     * Crear un nuevo registro de entrega
     */
    @PostMapping
    public ResponseEntity<ServeRecordDTO> create(@RequestBody ServeRecordDTO recordDTO) {
        logger.info("Creating new serve record for order ID: {}, vehicle ID: {}, volume: {} m3", 
                recordDTO.getOrderId(), recordDTO.getVehicleId(), recordDTO.getGlpVolumeM3());
        
        Optional<Vehicle> vehicleOpt = vehicleService.findById(recordDTO.getVehicleId());
        Optional<Order> orderOpt = orderService.findById(recordDTO.getOrderId());
        
        if (vehicleOpt.isEmpty() || orderOpt.isEmpty()) {
            if (vehicleOpt.isEmpty()) {
                logger.warn("Failed to create serve record: Vehicle with ID {} not found", recordDTO.getVehicleId());
            }
            if (orderOpt.isEmpty()) {
                logger.warn("Failed to create serve record: Order with ID {} not found", recordDTO.getOrderId());
            }
            return ResponseEntity.badRequest().build();
        }
        
        Vehicle vehicle = vehicleOpt.get();
        Order order = orderOpt.get();
        int glpVolume = recordDTO.getGlpVolumeM3();
        LocalDateTime serveDate = recordDTO.getServeDate() != null ? recordDTO.getServeDate() : LocalDateTime.now();
        
        ServeRecord serveRecord = new ServeRecord(vehicle, order, glpVolume, serveDate);
        ServeRecord savedRecord = serveRecordService.save(serveRecord);
        
        logger.info("Serve record created with ID: {}", savedRecord.getId());
        return new ResponseEntity<>(ServeRecordDTO.fromEntity(savedRecord), HttpStatus.CREATED);
    }
    
    /**
     * Eliminar un registro de entrega
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        logger.info("Attempting to delete serve record with ID: {}", id);
        return serveRecordService.findById(id)
                .map(record -> {
                    serveRecordService.deleteById(id);
                    logger.info("Serve record with ID: {} was deleted successfully", id);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> {
                    logger.warn("Serve record with ID: {} not found for deletion", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }
}

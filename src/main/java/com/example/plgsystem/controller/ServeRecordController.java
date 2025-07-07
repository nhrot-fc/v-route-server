package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.ServeRecordService;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.VehicleService;
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

    private final ServeRecordService serveRecordService;
    private final VehicleService vehicleService;
    private final OrderService orderService;

    public ServeRecordController(ServeRecordService serveRecordService, VehicleService vehicleService, OrderService orderService) {
        this.serveRecordService = serveRecordService;
        this.vehicleService = vehicleService;
        this.orderService = orderService;
    }

    /**
     * Obtener un registro de entrega por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServeRecordDTO> getById(@PathVariable UUID id) {
        Optional<ServeRecord> record = serveRecordService.findById(id);
        return record.map(r -> ResponseEntity.ok(ServeRecordDTO.fromEntity(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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

        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<ServeRecord> records;

            if (orderId != null) {
                records = serveRecordService.findByOrderId(orderId);
            } else if (vehicleId != null) {
                records = serveRecordService.findByVehicleId(vehicleId);
            } else if (startDate != null && endDate != null) {
                records = serveRecordService.findByServeDateBetween(startDate, endDate);
            } else {
                records = serveRecordService.findAll();
            }

            List<ServeRecordDTO> recordDTOs = records.stream()
                    .map(ServeRecordDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(recordDTOs);
        }

        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ServeRecord> recordsPage;

        if (orderId != null) {
            recordsPage = serveRecordService.findByOrderIdPaged(orderId, pageable);
        } else if (vehicleId != null) {
            recordsPage = serveRecordService.findByVehicleIdPaged(vehicleId, pageable);
        } else if (startDate != null && endDate != null) {
            recordsPage = serveRecordService.findByServeDateBetweenPaged(startDate, endDate, pageable);
        } else {
            recordsPage = serveRecordService.findAllPaged(pageable);
        }

        return ResponseEntity.ok(recordsPage.map(ServeRecordDTO::fromEntity));
    }
    
    /**
     * Crear un nuevo registro de entrega
     */
    @PostMapping
    public ResponseEntity<ServeRecordDTO> create(@RequestBody ServeRecordDTO recordDTO) {
        Optional<Vehicle> vehicleOpt = vehicleService.findById(recordDTO.getVehicleId());
        Optional<Order> orderOpt = orderService.findById(recordDTO.getOrderId());
        
        if (vehicleOpt.isEmpty() || orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Vehicle vehicle = vehicleOpt.get();
        Order order = orderOpt.get();
        int glpVolume = recordDTO.getGlpVolumeM3();
        LocalDateTime serveDate = recordDTO.getServeDate() != null ? recordDTO.getServeDate() : LocalDateTime.now();
        
        ServeRecord serveRecord = new ServeRecord(vehicle, order, glpVolume, serveDate);
        ServeRecord savedRecord = serveRecordService.save(serveRecord);
        
        return new ResponseEntity<>(ServeRecordDTO.fromEntity(savedRecord), HttpStatus.CREATED);
    }
    
    /**
     * Eliminar un registro de entrega
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return serveRecordService.findById(id)
                .map(record -> {
                    serveRecordService.deleteById(id);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}

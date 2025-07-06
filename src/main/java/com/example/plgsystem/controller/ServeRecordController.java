package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.service.ServeRecordService;
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
@RequestMapping("/api/serve-records")
public class ServeRecordController {

    private final ServeRecordService serveRecordService;

    public ServeRecordController(ServeRecordService serveRecordService) {
        this.serveRecordService = serveRecordService;
    }

    /**
     * Crear un nuevo registro de entrega
     */
    @PostMapping
    public ResponseEntity<ServeRecordDTO> create(@RequestBody ServeRecordDTO serveRecordDTO) {
        ServeRecord serveRecord = serveRecordDTO.toEntity();
        ServeRecord savedRecord = serveRecordService.save(serveRecord);
        return new ResponseEntity<>(ServeRecordDTO.fromEntity(savedRecord), HttpStatus.CREATED);
    }

    /**
     * Obtener un registro de entrega por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServeRecordDTO> getById(@PathVariable Long id) {
        Optional<ServeRecord> record = serveRecordService.findById(id);
        return record.map(r -> ResponseEntity.ok(ServeRecordDTO.fromEntity(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Listar todos los registros de entrega con opciones de filtrado y paginación opcional
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
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
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
     * Eliminar un registro de entrega por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return serveRecordService.findById(id)
                .map(record -> {
                    serveRecordService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.example.plgsystem.controller;

import com.example.plgsystem.dto.DeliveryRecordDTO;
import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.dto.VehicleDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.service.ServeRecordService;
import com.example.plgsystem.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final ServeRecordService serveRecordService;

    public VehicleController(VehicleService vehicleService, ServeRecordService serveRecordService) {
        this.vehicleService = vehicleService;
        this.serveRecordService = serveRecordService;
    }

    /**
     * Crear un nuevo vehículo
     */
    @PostMapping
    public ResponseEntity<VehicleDTO> create(@RequestBody VehicleDTO vehicleDTO) {
        Vehicle vehicle = vehicleDTO.toEntity();
        Vehicle savedVehicle = vehicleService.save(vehicle);
        return new ResponseEntity<>(VehicleDTO.fromEntity(savedVehicle), HttpStatus.CREATED);
    }

    /**
     * Obtener un vehículo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getById(@PathVariable String id) {
        Optional<Vehicle> vehicle = vehicleService.findById(id);
        return vehicle.map(v -> ResponseEntity.ok(VehicleDTO.fromEntity(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Listar todos los vehículos con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) Integer minGlp,
            @RequestParam(required = false) Double minFuel,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Vehicle> vehicles;
            
            if (type != null) {
                vehicles = vehicleService.findByType(type);
            } else if (status != null) {
                vehicles = vehicleService.findByStatus(status);
            } else if (minGlp != null) {
                vehicles = vehicleService.findByMinimumGlp(minGlp);
            } else if (minFuel != null) {
                vehicles = vehicleService.findByMinimumFuel(minFuel);
            } else {
                vehicles = vehicleService.findAll();
            }
            
            List<VehicleDTO> vehicleDTOs = vehicles.stream()
                    .map(VehicleDTO::fromEntity)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(vehicleDTOs);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Vehicle> vehicles;
        
        if (type != null) {
            vehicles = vehicleService.findByTypePaged(type, pageable);
        } else if (status != null) {
            vehicles = vehicleService.findByStatusPaged(status, pageable);
        } else if (minGlp != null) {
            vehicles = vehicleService.findByMinimumGlpPaged(minGlp, pageable);
        } else if (minFuel != null) {
            vehicles = vehicleService.findByMinimumFuelPaged(minFuel, pageable);
        } else {
            vehicles = vehicleService.findAllPaged(pageable);
        }
        
        Page<VehicleDTO> vehicleDTOs = vehicles.map(VehicleDTO::fromEntity);
                
        return ResponseEntity.ok(vehicleDTOs);
    }

    /**
     * Actualizar un vehículo existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleDTO> update(@PathVariable String id, @RequestBody VehicleDTO vehicleDTO) {
        return vehicleService.findById(id)
                .map(existingVehicle -> {
                    // Actualizar solo los campos permitidos
                    existingVehicle.setCurrentPosition(vehicleDTO.getCurrentPosition());
                    existingVehicle.setStatus(vehicleDTO.getStatus());
                    
                    Vehicle updatedVehicle = vehicleService.save(existingVehicle);
                    return ResponseEntity.ok(VehicleDTO.fromEntity(updatedVehicle));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Eliminar un vehículo por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return vehicleService.findById(id)
                .map(vehicle -> {
                    vehicleService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Recargar combustible de un vehículo
     */
    @PostMapping("/{id}/refuel")
    public ResponseEntity<VehicleDTO> refuel(@PathVariable String id) {
        return vehicleService.refuelVehicle(id)
                .map(vehicle -> ResponseEntity.ok(VehicleDTO.fromEntity(vehicle)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Recargar GLP de un vehículo
     */
    @PostMapping("/{id}/refill")
    public ResponseEntity<VehicleDTO> refillGlp(@PathVariable String id, @RequestParam int volumeM3) {
        return vehicleService.refillGlp(id, volumeM3)
                .map(vehicle -> ResponseEntity.ok(VehicleDTO.fromEntity(vehicle)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Realizar entrega de un pedido
     */
    @PostMapping("/{id}/serve")
    public ResponseEntity<ServeRecordDTO> serveOrder(
            @PathVariable String id, 
            @RequestParam String orderId,
            @RequestBody DeliveryRecordDTO deliveryRecord) {
        
        LocalDateTime deliveryTime = deliveryRecord.getServeDate() != null ? 
                deliveryRecord.getServeDate() : LocalDateTime.now();
        
        return vehicleService.serveOrder(id, orderId, deliveryRecord.getVolumeM3(), deliveryTime)
                .map(serveRecord -> {
                    ServeRecord savedRecord = serveRecordService.save(serveRecord);
                    return ResponseEntity.ok(ServeRecordDTO.fromEntity(savedRecord));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Actualizar la posición de un vehículo y consumir combustible según la distancia
     */
    @PostMapping("/{id}/move")
    public ResponseEntity<VehicleDTO> moveVehicle(
            @PathVariable String id,
            @RequestParam double distanceKm) {
        
        return vehicleService.moveVehicle(id, distanceKm)
                .map(vehicle -> ResponseEntity.ok(VehicleDTO.fromEntity(vehicle)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

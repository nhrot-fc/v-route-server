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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(VehicleController.class);
    private final VehicleService vehicleService;
    private final ServeRecordService serveRecordService;

    public VehicleController(VehicleService vehicleService, ServeRecordService serveRecordService) {
        this.vehicleService = vehicleService;
        this.serveRecordService = serveRecordService;
        logger.info("VehicleController initialized");
    }

    /**
     * Crear un nuevo vehículo
     */
    @PostMapping
    public ResponseEntity<VehicleDTO> create(@RequestBody VehicleDTO vehicleDTO) {
        logger.info("Creating new vehicle with ID: {}, type: {}", vehicleDTO.getId(), vehicleDTO.getType());
        Vehicle vehicle = vehicleDTO.toEntity();
        Vehicle savedVehicle = vehicleService.save(vehicle);
        logger.info("Vehicle created with ID: {}", savedVehicle.getId());
        return new ResponseEntity<>(VehicleDTO.fromEntity(savedVehicle), HttpStatus.CREATED);
    }

    /**
     * Obtener un vehículo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getById(@PathVariable String id) {
        logger.info("Fetching vehicle with ID: {}", id);
        Optional<Vehicle> vehicle = vehicleService.findById(id);
        if (vehicle.isPresent()) {
            logger.info("Vehicle found with ID: {}", id);
            return ResponseEntity.ok(vehicle.get());
        } else {
            logger.warn("Vehicle with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
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
        
        logger.info("Listing vehicles with filters - type: {}, status: {}, minGlp: {}, minFuel: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                type, status, minGlp, minFuel, paginated, page, size, sortBy, direction);
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Vehicle> vehicles;
            
            if (type != null) {
                logger.info("Filtering vehicles by type: {}", type);
                vehicles = vehicleService.findByType(type);
            } else if (status != null) {
                logger.info("Filtering vehicles by status: {}", status);
                vehicles = vehicleService.findByStatus(status);
            } else if (minGlp != null) {
                logger.info("Filtering vehicles by minimum GLP: {}", minGlp);
                vehicles = vehicleService.findByMinimumGlp(minGlp);
            } else if (minFuel != null) {
                logger.info("Filtering vehicles by minimum fuel: {}", minFuel);
                vehicles = vehicleService.findByMinimumFuel(minFuel);
            } else {
                logger.info("Retrieving all vehicles without filtering");
                vehicles = vehicleService.findAll();
            }
            
            List<VehicleDTO> vehicleDTOs = vehicles.stream()
                    .map(VehicleDTO::fromEntity)
                    .collect(Collectors.toList());
            
            logger.info("Found {} vehicles matching criteria", vehicleDTOs.size());
            return ResponseEntity.ok(vehicleDTOs);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Vehicle> vehicles;
        
        if (type != null) {
            logger.info("Filtering paginated vehicles by type: {}", type);
            vehicles = vehicleService.findByTypePaged(type, pageable);
        } else if (status != null) {
            logger.info("Filtering paginated vehicles by status: {}", status);
            vehicles = vehicleService.findByStatusPaged(status, pageable);
        } else if (minGlp != null) {
            logger.info("Filtering paginated vehicles by minimum GLP: {}", minGlp);
            vehicles = vehicleService.findByMinimumGlpPaged(minGlp, pageable);
        } else if (minFuel != null) {
            logger.info("Filtering paginated vehicles by minimum fuel: {}", minFuel);
            vehicles = vehicleService.findByMinimumFuelPaged(minFuel, pageable);
        } else {
            logger.info("Retrieving all paginated vehicles without filtering");
            vehicles = vehicleService.findAllPaged(pageable);
        }
        
        Page<VehicleDTO> vehicleDTOs = vehicles.map(VehicleDTO::fromEntity);
        
        logger.info("Found page {} of {} with {} vehicles per page (total: {})", 
                vehicles.getNumber(), vehicles.getTotalPages(), vehicles.getSize(), vehicles.getTotalElements());
        return ResponseEntity.ok(vehicleDTOs);
    }

    /**
     * Actualizar un vehículo existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleDTO> update(@PathVariable String id, @RequestBody VehicleDTO vehicleDTO) {
        logger.info("Updating vehicle with ID: {}", id);
        return vehicleService.findById(id)
                .map(existingVehicle -> {
                    // Actualizar solo los campos permitidos
                    existingVehicle.setCurrentPosition(vehicleDTO.getCurrentPosition());
                    existingVehicle.setStatus(vehicleDTO.getStatus());
                    
                    Vehicle updatedVehicle = vehicleService.save(existingVehicle);
                    logger.info("Vehicle with ID: {} was updated successfully", id);
                    return ResponseEntity.ok(VehicleDTO.fromEntity(updatedVehicle));
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle with ID: {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Eliminar un vehículo por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("Attempting to delete vehicle with ID: {}", id);
        return vehicleService.findById(id)
                .map(vehicle -> {
                    vehicleService.deleteById(id);
                    logger.info("Vehicle with ID: {} was deleted successfully", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle with ID: {} not found for deletion", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Recargar combustible de un vehículo
     */
    @PostMapping("/{id}/refuel")
    public ResponseEntity<VehicleDTO> refuel(@PathVariable String id) {
        logger.info("Refueling vehicle with ID: {}", id);
        return vehicleService.refuelVehicle(id)
                .map(vehicle -> {
                    logger.info("Vehicle with ID: {} was refueled successfully", id);
                    return ResponseEntity.ok(VehicleDTO.fromEntity(vehicle));
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle with ID: {} not found for refueling", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Recargar GLP de un vehículo
     */
    @PostMapping("/{id}/refill")
    public ResponseEntity<VehicleDTO> refillGlp(@PathVariable String id, @RequestParam int volumeM3) {
        logger.info("Refilling GLP for vehicle with ID: {}, volume: {} m3", id, volumeM3);
        return vehicleService.refillGlp(id, volumeM3)
                .map(vehicle -> {
                    logger.info("Vehicle with ID: {} was refilled with {} m3 of GLP successfully", id, volumeM3);
                    return ResponseEntity.ok(VehicleDTO.fromEntity(vehicle));
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle with ID: {} not found for GLP refilling", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Realizar entrega de un pedido
     */
    @PostMapping("/{id}/serve")
    public ResponseEntity<ServeRecordDTO> serveOrder(
            @PathVariable String id, 
            @RequestParam String orderId,
            @RequestBody DeliveryRecordDTO deliveryRecord) {
        
        logger.info("Vehicle with ID: {} serving order: {}, volume: {} m3", id, orderId, deliveryRecord.getVolumeM3());
        LocalDateTime deliveryTime = deliveryRecord.getServeDate() != null ? 
                deliveryRecord.getServeDate() : LocalDateTime.now();
        
        return vehicleService.serveOrder(id, orderId, deliveryRecord.getVolumeM3(), deliveryTime)
                .map(serveRecord -> {
                    ServeRecord savedRecord = serveRecordService.save(serveRecord);
                    logger.info("Vehicle with ID: {} successfully served order: {}, serve record ID: {}", 
                            id, orderId, savedRecord.getId());
                    return ResponseEntity.ok(ServeRecordDTO.fromEntity(savedRecord));
                })
                .orElseGet(() -> {
                    logger.warn("Failed to serve order: {} with vehicle: {}", orderId, id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Actualizar la posición de un vehículo y consumir combustible según la distancia
     */
    @PostMapping("/{id}/move")
    public ResponseEntity<VehicleDTO> moveVehicle(
            @PathVariable String id,
            @RequestParam double distanceKm) {
        
        logger.info("Moving vehicle with ID: {} by distance: {} km", id, distanceKm);
        return vehicleService.moveVehicle(id, distanceKm)
                .map(vehicle -> {
                    logger.info("Vehicle with ID: {} was moved successfully, new position: {}, remaining fuel: {} gal", 
                            id, vehicle.getCurrentPosition(), vehicle.getCurrentFuelGal());
                    return ResponseEntity.ok(VehicleDTO.fromEntity(vehicle));
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle with ID: {} not found for moving operation", id);
                    return ResponseEntity.notFound().build();
                });
    }
}

package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.service.DepotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/depots")
public class DepotController {

    private static final Logger logger = LoggerFactory.getLogger(DepotController.class);
    private final DepotService depotService;

    public DepotController(DepotService depotService) {
        this.depotService = depotService;
        logger.info("DepotController initialized");
    }

    /**
     * Crear un nuevo depósito
     */
    @PostMapping
    public ResponseEntity<Depot> create(@RequestBody Depot depot) {
        logger.info("Creating new depot with ID: {}, type: {}", 
                depot.getId(), depot.getType());
        Depot savedDepot = depotService.save(depot);
        logger.info("Depot created with ID: {}", savedDepot.getId());
        return new ResponseEntity<>(savedDepot, HttpStatus.CREATED);
    }

    /**
     * Actualizar un depósito existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Depot> update(@PathVariable String id, @RequestBody Depot depot) {
        logger.info("Updating depot with ID: {}", id);
        return depotService.findById(id)
                .map(existingDepot -> {
                    // El ID lo establece el cliente, pues es un String, solo verificamos que exista
                    Depot updatedDepot = depotService.save(depot);
                    logger.info("Depot with ID: {} was updated successfully", id);
                    return ResponseEntity.ok(updatedDepot);
                })
                .orElseGet(() -> {
                    logger.warn("Depot with ID: {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Obtener un depósito por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Depot> getById(@PathVariable String id) {
        logger.info("Fetching depot with ID: {}", id);
        Optional<Depot> depot = depotService.findById(id);
        if (depot.isPresent()) {
            logger.info("Depot found with ID: {}", id);
            return ResponseEntity.ok(depot.get());
        } else {
            logger.warn("Depot with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los depósitos con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) DepotType type,
            @RequestParam(required = false) Boolean isMain,
            @RequestParam(required = false) Integer minGlpCapacity,
            @RequestParam(required = false) Integer minCurrentGlp,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        logger.info("Listing depots with filters - type: {}, isMain: {}, minGlpCapacity: {}, minCurrentGlp: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                type, isMain, minGlpCapacity, minCurrentGlp, paginated, page, size, sortBy, direction);
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Depot> depots;
            
            if (Boolean.TRUE.equals(isMain)) {
                logger.info("Filtering depots by main depot status: true");
                depots = depotService.findMainDepots();
            } else if (Boolean.FALSE.equals(isMain)) {
                logger.info("Filtering depots by auxiliary depot status");
                depots = depotService.findAuxiliaryDepots();
            } else if (type != null) {
                logger.info("Filtering depots by type: {}", type);
                depots = depotService.findByType(type);
            } else if (minGlpCapacity != null) {
                // Filtrar por capacidad mínima
                logger.info("Filtering depots by minimum capacity: {}", minGlpCapacity);
                depots = depotService.findByMinCapacity(minGlpCapacity);
            } else if (minCurrentGlp != null) {
                // Filtrar por GLP disponible mínimo
                logger.info("Filtering depots by minimum current GLP: {}", minCurrentGlp);
                depots = depotService.findByMinCurrentGlp(minCurrentGlp);
            } else {
                // Sin filtros, retornar todos
                logger.info("Retrieving all depots without filtering");
                depots = depotService.findAll();
            }
            
            logger.info("Found {} depots matching criteria", depots.size());
            return ResponseEntity.ok(depots);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Depot> depots;
        
        if (type != null) {
            logger.info("Filtering paginated depots by type: {}", type);
            depots = depotService.findByTypePaged(type, pageable);
        } else if (minGlpCapacity != null) {
            // Filtrar por capacidad mínima
            logger.info("Filtering paginated depots by minimum capacity: {}", minGlpCapacity);
            depots = depotService.findByMinCapacityPaged(minGlpCapacity, pageable);
        } else if (minCurrentGlp != null) {
            // Filtrar por GLP disponible mínimo
            logger.info("Filtering paginated depots by minimum current GLP: {}", minCurrentGlp);
            depots = depotService.findByMinCurrentGlpPaged(minCurrentGlp, pageable);
        } else {
            // Sin filtros, retornar todos
            logger.info("Retrieving all paginated depots without filtering");
            depots = depotService.findAllPaged(pageable);
        }
        
        logger.info("Found page {} of {} with {} depots per page (total: {})", 
                depots.getNumber(), depots.getTotalPages(), depots.getSize(), depots.getTotalElements());
        return ResponseEntity.ok(depots);
    }

    /**
     * Eliminar un depósito por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("Attempting to delete depot with ID: {}", id);
        return depotService.findById(id)
                .map(depot -> {
                    depotService.deleteById(id);
                    logger.info("Depot with ID: {} was deleted successfully", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    logger.warn("Depot with ID: {} not found for deletion", id);
                    return ResponseEntity.notFound().build();
                });
    }
}

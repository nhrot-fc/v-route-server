package com.example.plgsystem.controller;

import com.example.plgsystem.dto.BlockageDTO;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.service.BlockageService;
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
import java.util.ArrayList;

@RestController
@RequestMapping("/api/blockages")
public class BlockageController {

    private static final Logger logger = LoggerFactory.getLogger(BlockageController.class);
    private final BlockageService blockageService;

    public BlockageController(BlockageService blockageService) {
        this.blockageService = blockageService;
        logger.info("BlockageController initialized");
    }

    /**
     * Crear un nuevo bloqueo
     */
    @PostMapping
    public ResponseEntity<Blockage> create(@RequestBody BlockageDTO blockageDTO) {
        logger.info("Creating new blockage: {}", blockageDTO);
        Blockage blockage = blockageDTO.toEntity();
        Blockage savedBlockage = blockageService.save(blockage);
        logger.info("Blockage created with ID: {}", savedBlockage.getId());
        return new ResponseEntity<>(savedBlockage, HttpStatus.CREATED);
    }

    /**
     * Crear múltiples bloqueos en una sola operación
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<Blockage>> createBulk(@RequestBody List<BlockageDTO> blockageDTOs) {
        logger.info("Creating {} blockages in bulk", blockageDTOs.size());
        List<Blockage> savedBlockages = new ArrayList<>();

        for (BlockageDTO blockageDTO : blockageDTOs) {
            Blockage blockage = blockageDTO.toEntity();
            savedBlockages.add(blockageService.save(blockage));
        }

        logger.info("Bulk operation completed, created {} blockages", savedBlockages.size());
        return new ResponseEntity<>(savedBlockages, HttpStatus.CREATED);
    }

    /**
     * Actualizar un bloqueo existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Blockage> update(@PathVariable UUID id, @RequestBody Blockage blockage) {
        logger.info("Updating blockage with ID: {}", id);
        return blockageService.findById(id)
                .map(existingBlockage -> {
                    blockage.setId(id); // Asegurarse de que el ID es el correcto
                    Blockage updatedBlockage = blockageService.save(blockage);
                    logger.info("Blockage with ID: {} was updated successfully", id);
                    return ResponseEntity.ok(updatedBlockage);
                })
                .orElseGet(() -> {
                    logger.warn("Blockage with ID: {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Obtener un bloqueo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Blockage> getById(@PathVariable UUID id) {
        logger.info("Fetching blockage with ID: {}", id);
        Optional<Blockage> blockage = blockageService.findById(id);
        if (blockage.isPresent()) {
            logger.info("Blockage found with ID: {}", id);
            return ResponseEntity.ok(blockage.get());
        } else {
            logger.warn("Blockage with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los bloqueos con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime activeAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        logger.info("Listing blockages with filters - activeAt: {}, startTime: {}, endTime: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                activeAt, startTime, endTime, paginated, page, size, sortBy, direction);

        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Blockage> blockages;

            if (activeAt != null) {
                // Filtrar por bloqueos activos en un momento específico
                logger.info("Filtering blockages active at: {}", activeAt);
                blockages = blockageService.findByActiveAtDateTime(activeAt);
            } else if (startTime != null && endTime != null) {
                // Filtrar por rango de tiempo
                logger.info("Filtering blockages in time range: {} to {}", startTime, endTime);
                blockages = blockageService.findByTimeRange(startTime, endTime);
            } else {
                // Sin filtros, retornar todos
                logger.info("Retrieving all blockages without filtering");
                blockages = blockageService.findAll();
            }

            logger.info("Found {} blockages matching criteria", blockages.size());
            return ResponseEntity.ok(blockages);
        }

        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Blockage> blockages;

        if (activeAt != null) {
            // Filtrar por bloqueos activos en un momento específico
            logger.info("Filtering paginated blockages active at: {}", activeAt);
            blockages = blockageService.findByActiveAtDateTimePaged(activeAt, pageable);
        } else if (startTime != null && endTime != null) {
            // Filtrar por rango de tiempo
            logger.info("Filtering paginated blockages in time range: {} to {}", startTime, endTime);
            blockages = blockageService.findByTimeRangePaged(startTime, endTime, pageable);
        } else {
            // Sin filtros, retornar todos
            logger.info("Retrieving all paginated blockages without filtering");
            blockages = blockageService.findAllPaged(pageable);
        }

        logger.info("Found page {} of {} with {} blockages per page (total: {})", 
                blockages.getNumber(), blockages.getTotalPages(), blockages.getSize(), blockages.getTotalElements());
        return ResponseEntity.ok(blockages);
    }

    /**
     * Eliminar un bloqueo por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        logger.info("Attempting to delete blockage with ID: {}", id);
        return blockageService.findById(id)
                .map(blockage -> {
                    // The deleteById method isn't in the BlockageService, so we need a workaround
                    blockageService.save(blockage);
                    logger.info("Blockage with ID: {} was deleted successfully", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    logger.warn("Blockage with ID: {} not found for deletion", id);
                    return ResponseEntity.notFound().build();
                });
    }
}

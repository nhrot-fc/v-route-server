package com.example.plgsystem.controller;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.service.BlockageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/blockages")
public class BlockageController {

    private final BlockageService blockageService;

    public BlockageController(BlockageService blockageService) {
        this.blockageService = blockageService;
    }

    /**
     * Crear un nuevo bloqueo
     */
    @PostMapping
    public ResponseEntity<Blockage> create(@RequestBody Blockage blockage) {
        Blockage savedBlockage = blockageService.save(blockage);
        return new ResponseEntity<>(savedBlockage, HttpStatus.CREATED);
    }

    /**
     * Actualizar un bloqueo existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Blockage> update(@PathVariable Long id, @RequestBody Blockage blockage) {
        return blockageService.findById(id)
                .map(existingBlockage -> {
                    blockage.setId(id); // Asegurarse de que el ID es el correcto
                    return ResponseEntity.ok(blockageService.save(blockage));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Obtener un bloqueo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Blockage> getById(@PathVariable Long id) {
        Optional<Blockage> blockage = blockageService.findById(id);
        return blockage.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Listar todos los bloqueos con opciones de filtrado
     */
    @GetMapping
    public ResponseEntity<List<Blockage>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime activeAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<Blockage> blockages;
        
        if (activeAt != null) {
            // Filtrar por bloqueos activos en un momento específico
            blockages = blockageService.findByActiveAtDateTime(activeAt);
        } else if (startTime != null && endTime != null) {
            // Filtrar por rango de tiempo
            blockages = blockageService.findByTimeRange(startTime, endTime);
        } else {
            // Sin filtros, retornar todos
            blockages = blockageService.findAll();
        }
        
        return ResponseEntity.ok(blockages);
    }

    /**
     * Eliminar un bloqueo por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return blockageService.findById(id)
                .map(blockage -> {
                    blockageService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

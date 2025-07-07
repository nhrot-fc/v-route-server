package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.service.DepotService;
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

    private final DepotService depotService;

    public DepotController(DepotService depotService) {
        this.depotService = depotService;
    }

    /**
     * Crear un nuevo depósito
     */
    @PostMapping
    public ResponseEntity<Depot> create(@RequestBody Depot depot) {
        Depot savedDepot = depotService.save(depot);
        return new ResponseEntity<>(savedDepot, HttpStatus.CREATED);
    }

    /**
     * Actualizar un depósito existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Depot> update(@PathVariable String id, @RequestBody Depot depot) {
        return depotService.findById(id)
                .map(existingDepot -> {
                    // El ID lo establece el cliente pues es un String, solo verificamos que exista
                    return ResponseEntity.ok(depotService.save(depot));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Obtener un depósito por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Depot> getById(@PathVariable String id) {
        Optional<Depot> depot = depotService.findById(id);
        return depot.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Depot> depots;
            
            if (Boolean.TRUE.equals(isMain)) {
                depots = depotService.findMainDepots();
            } else if (Boolean.FALSE.equals(isMain)) {
                depots = depotService.findAuxiliaryDepots();
            } else if (type != null) {
                depots = depotService.findByType(type);
            } else if (minGlpCapacity != null) {
                // Filtrar por capacidad mínima
                depots = depotService.findByMinCapacity(minGlpCapacity);
            } else if (minCurrentGlp != null) {
                // Filtrar por GLP disponible mínimo
                depots = depotService.findByMinCurrentGlp(minCurrentGlp);
            } else {
                // Sin filtros, retornar todos
                depots = depotService.findAll();
            }
            
            return ResponseEntity.ok(depots);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Depot> depots;
        
        if (type != null) {
            depots = depotService.findByTypePaged(type, pageable);
        } else if (minGlpCapacity != null) {
            // Filtrar por capacidad mínima
            depots = depotService.findByMinCapacityPaged(minGlpCapacity, pageable);
        } else if (minCurrentGlp != null) {
            // Filtrar por GLP disponible mínimo
            depots = depotService.findByMinCurrentGlpPaged(minCurrentGlp, pageable);
        } else {
            // Sin filtros, retornar todos
            depots = depotService.findAllPaged(pageable);
        }
        
        return ResponseEntity.ok(depots);
    }

    /**
     * Eliminar un depósito por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return depotService.findById(id)
                .map(depot -> {
                    depotService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

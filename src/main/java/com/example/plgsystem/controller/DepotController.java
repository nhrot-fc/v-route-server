package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.service.DepotService;
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
     * Listar todos los depósitos con opciones de filtrado
     */
    @GetMapping
    public ResponseEntity<List<Depot>> list(
            @RequestParam(required = false) Boolean canRefuel,
            @RequestParam(required = false) Integer minGlpCapacity,
            @RequestParam(required = false) Integer minCurrentGlp) {
        
        List<Depot> depots;
        
        if (canRefuel != null) {
            // Filtrar por capacidad de recarga
            depots = depotService.findByCanRefuel(canRefuel);
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

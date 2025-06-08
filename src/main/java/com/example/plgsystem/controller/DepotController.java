package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.DepotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/depots")
@Tag(name = "Depots", description = "Gestión de depósitos de GLP del sistema PLG")
public class DepotController {

    @Autowired
    private DepotRepository depotRepository;

    @Operation(summary = "Obtener todos los depósitos", description = "Retorna la lista completa de depósitos registrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de depósitos obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) })
    })
    @GetMapping
    public List<Depot> getAllDepots() {
        return depotRepository.findAll();
    }

    @Operation(summary = "Obtener depósito por ID", description = "Retorna un depósito específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Depósito encontrado",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) }),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Depot> getDepotById(
            @Parameter(description = "ID del depósito") @PathVariable String id) {
        Optional<Depot> depot = depotRepository.findById(id);
        return depot.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener depósitos con GLP suficiente", description = "Retorna depósitos que tienen al menos la cantidad requerida de GLP")
    @GetMapping("/sufficient-glp/{requiredGLP}")
    public List<Depot> getDepotsWithSufficientGLP(
            @Parameter(description = "Cantidad mínima de GLP requerida") @PathVariable double requiredGLP) {
        return depotRepository.findDepotsWithSufficientGLP(requiredGLP);
    }

    @Operation(summary = "Obtener depósitos por rango de ubicación", description = "Retorna depósitos dentro de un rango geográfico específico")
    @GetMapping("/location-range")
    public List<Depot> getDepotsByLocationRange(
            @Parameter(description = "Coordenada X mínima") @RequestParam int minX, 
            @Parameter(description = "Coordenada X máxima") @RequestParam int maxX,
            @Parameter(description = "Coordenada Y mínima") @RequestParam int minY, 
            @Parameter(description = "Coordenada Y máxima") @RequestParam int maxY) {
        return depotRepository.findDepotsByLocationRange(minX, maxX, minY, maxY);
    }

    @Operation(summary = "Rellenar depósito", description = "Rellena un depósito a su capacidad máxima")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Depósito rellenado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) }),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @PutMapping("/{id}/refill")
    public ResponseEntity<Depot> refillDepot(
            @Parameter(description = "ID del depósito") @PathVariable String id) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isPresent()) {
            Depot depot = optionalDepot.get();
            depot.refill();
            Depot updatedDepot = depotRepository.save(depot);
            return ResponseEntity.ok(updatedDepot);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Servir GLP desde depósito", description = "Retira una cantidad específica de GLP del depósito")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GLP servido exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) }),
        @ApiResponse(responseCode = "400", description = "Cantidad solicitada no disponible"),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @PutMapping("/{id}/serve")
    public ResponseEntity<Depot> serveFromDepot(
            @Parameter(description = "ID del depósito") @PathVariable String id, 
            @Parameter(description = "Cantidad de GLP solicitada") @RequestParam double requestedGLP) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isPresent()) {
            Depot depot = optionalDepot.get();
            if (depot.canServe(requestedGLP)) {
                depot.serve(requestedGLP);
                Depot updatedDepot = depotRepository.save(depot);
                return ResponseEntity.ok(updatedDepot);
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Obtener capacidad total de almacenamiento", description = "Retorna la capacidad total de almacenamiento de todos los depósitos")
    @GetMapping("/capacity/total")
    public ResponseEntity<Double> getTotalStorageCapacity() {
        Double capacity = depotRepository.getTotalStorageCapacity();
        return ResponseEntity.ok(capacity != null ? capacity : 0.0);
    }

    @Operation(summary = "Obtener GLP total actual", description = "Retorna la cantidad total de GLP actualmente almacenada en todos los depósitos")
    @GetMapping("/glp/current-total")
    public ResponseEntity<Double> getCurrentTotalGLP() {
        Double glp = depotRepository.getCurrentTotalGLP();
        return ResponseEntity.ok(glp != null ? glp : 0.0);
    }

    @Operation(summary = "Obtener depósitos con capacidad disponible", description = "Retorna depósitos que tienen al menos la capacidad libre mínima especificada")
    @GetMapping("/available")
    public List<Depot> getDepotsWithAvailableCapacity(
            @Parameter(description = "Capacidad mínima libre requerida") @RequestParam double minCapacity) {
        return depotRepository.findAll().stream()
                .filter(depot -> (depot.getGlpCapacity() - depot.getCurrentGLP()) >= minCapacity)
                .toList();
    }

    @Operation(summary = "Obtener depósitos por radio", description = "Retorna depósitos dentro de un radio específico desde una posición")
    @GetMapping("/radius")
    public List<Depot> getDepotsByRadius(
            @Parameter(description = "Coordenada X del centro") @RequestParam int x, 
            @Parameter(description = "Coordenada Y del centro") @RequestParam int y, 
            @Parameter(description = "Radio de búsqueda") @RequestParam double radius) {
        return depotRepository.findAll().stream()
                .filter(depot -> depot.getPosition().distanceTo(new Position(x, y)) <= radius)
                .toList();
    }

    @Operation(summary = "Crear nuevo depósito", description = "Registra un nuevo depósito en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Depósito creado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) })
    })
    @PostMapping
    public ResponseEntity<Depot> createDepot(@RequestBody Depot depot) {
        Depot savedDepot = depotRepository.save(depot);
        return ResponseEntity.ok(savedDepot);
    }

    @Operation(summary = "Actualizar nivel de GLP", description = "Ajusta el nivel de GLP de un depósito (puede ser positivo o negativo)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nivel de GLP actualizado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) }),
        @ApiResponse(responseCode = "400", description = "Nivel resultante fuera de los límites permitidos"),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @PutMapping("/{id}/glp")
    public ResponseEntity<Depot> updateGLPLevel(
            @Parameter(description = "ID del depósito") @PathVariable String id, 
            @Parameter(description = "Cantidad a agregar/restar al nivel actual") @RequestParam double amount) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isPresent()) {
            Depot depot = optionalDepot.get();
            double newLevel = depot.getCurrentGLP() + amount;
            
            // Check bounds
            if (newLevel < 0 || newLevel > depot.getGlpCapacity()) {
                return ResponseEntity.badRequest().build();
            }
            
            depot.setCurrentGLP(newLevel);
            Depot updatedDepot = depotRepository.save(depot);
            return ResponseEntity.ok(updatedDepot);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Actualizar umbral mínimo", description = "Actualiza el umbral mínimo de GLP para un depósito")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Umbral actualizado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Depot.class)) }),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @PutMapping("/{id}/threshold")
    public ResponseEntity<Depot> updateMinimumThreshold(
            @Parameter(description = "ID del depósito") @PathVariable String id, 
            @Parameter(description = "Nuevo umbral mínimo") @RequestParam double threshold) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isPresent()) {
            Depot depot = optionalDepot.get();
            depot.setGlpMinThreshold(threshold);
            Depot updatedDepot = depotRepository.save(depot);
            return ResponseEntity.ok(updatedDepot);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Eliminar depósito", description = "Elimina un depósito del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Depósito eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepot(
            @Parameter(description = "ID del depósito a eliminar") @PathVariable String id) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isPresent()) {
            depotRepository.delete(optionalDepot.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

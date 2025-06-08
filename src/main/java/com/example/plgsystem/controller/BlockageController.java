package com.example.plgsystem.controller;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.repository.BlockageRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/blockages")
@Tag(name = "Blockages", description = "Gestión de bloqueos de rutas del sistema PLG")
public class BlockageController {

    @Autowired
    private BlockageRepository blockageRepository;

    @Operation(summary = "Obtener todos los bloqueos", description = "Retorna la lista completa de bloqueos registrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de bloqueos obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Blockage.class)) })
    })
    @GetMapping
    public List<Blockage> getAllBlockages() {
        return blockageRepository.findAll();
    }

    @Operation(summary = "Obtener bloqueo por ID", description = "Retorna un bloqueo específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bloqueo encontrado",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Blockage.class)) }),
        @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Blockage> getBlockageById(
            @Parameter(description = "ID del bloqueo") @PathVariable Long id) {
        Optional<Blockage> blockage = blockageRepository.findById(id);
        return blockage.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener bloqueos activos", description = "Retorna todos los bloqueos actualmente activos")
    @GetMapping("/active")
    public List<Blockage> getActiveBlockages() {
        return blockageRepository.findActiveBlockages(LocalDateTime.now());
    }

    @Operation(summary = "Obtener bloqueos activos en fecha específica", description = "Retorna bloqueos activos en una fecha y hora específica")
    @GetMapping("/active/{dateTime}")
    public List<Blockage> getActiveBlockagesAt(
            @Parameter(description = "Fecha y hora para consultar (ISO 8601)") @PathVariable String dateTime) {
        LocalDateTime time = LocalDateTime.parse(dateTime);
        return blockageRepository.findActiveBlockages(time);
    }

    @Operation(summary = "Obtener bloqueos por rango de fechas", description = "Retorna bloqueos en un rango de fechas específico")
    @GetMapping("/date-range")
    public List<Blockage> getBlockagesByDateRange(
            @Parameter(description = "Fecha de inicio (ISO 8601)") @RequestParam String startDate, 
            @Parameter(description = "Fecha de fin (ISO 8601)") @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return blockageRepository.findBlockagesByDateRange(start, end);
    }

    @Operation(summary = "Obtener bloqueos en segmento", description = "Retorna bloqueos que afectan un segmento específico de ruta")
    @GetMapping("/segment")
    public List<Blockage> getBlockagesForSegment(
            @Parameter(description = "Coordenada X del punto inicial") @RequestParam int x1, 
            @Parameter(description = "Coordenada Y del punto inicial") @RequestParam int y1,
            @Parameter(description = "Coordenada X del punto final") @RequestParam int x2, 
            @Parameter(description = "Coordenada Y del punto final") @RequestParam int y2) {
        return blockageRepository.findBlockagesForSegment(x1, y1, x2, y2);
    }

    @Operation(summary = "Crear nuevo bloqueo", description = "Registra un nuevo bloqueo en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bloqueo creado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Blockage.class)) })
    })
    @PostMapping
    public Blockage createBlockage(@RequestBody Blockage blockage) {
        return blockageRepository.save(blockage);
    }

    @Operation(summary = "Eliminar bloqueo", description = "Elimina un bloqueo del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bloqueo eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlockage(
            @Parameter(description = "ID del bloqueo a eliminar") @PathVariable Long id) {
        if (blockageRepository.existsById(id)) {
            blockageRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

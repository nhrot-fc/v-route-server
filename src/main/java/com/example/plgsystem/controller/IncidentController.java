package com.example.plgsystem.controller;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.IncidentType;
import com.example.plgsystem.repository.IncidentRepository;
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
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Gestión de incidentes del sistema PLG")
public class IncidentController {

    @Autowired
    private IncidentRepository incidentRepository;

    @Operation(summary = "Obtener todos los incidentes", description = "Retorna la lista completa de incidentes registrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de incidentes obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Incident.class)) })
    })
    @GetMapping
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Operation(summary = "Obtener incidente por ID", description = "Retorna un incidente específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Incidente encontrado",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Incident.class)) }),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(
            @Parameter(description = "ID del incidente") @PathVariable String id) {
        Optional<Incident> incident = incidentRepository.findById(id);
        return incident.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener incidentes por vehículo", description = "Retorna todos los incidentes de un vehículo específico")
    @GetMapping("/vehicle/{vehicleId}")
    public List<Incident> getIncidentsByVehicle(
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        return incidentRepository.findByVehicleId(vehicleId);
    }

    @Operation(summary = "Obtener incidentes por tipo", description = "Retorna incidentes filtrados por tipo")
    @GetMapping("/type/{type}")
    public List<Incident> getIncidentsByType(
            @Parameter(description = "Tipo de incidente") @PathVariable IncidentType type) {
        return incidentRepository.findByType(type);
    }

    @Operation(summary = "Obtener incidentes activos", description = "Retorna todos los incidentes actualmente activos (no resueltos)")
    @GetMapping("/active")
    public List<Incident> getActiveIncidents() {
        return incidentRepository.findActiveIncidents(LocalDateTime.now());
    }

    @Operation(summary = "Obtener incidentes activos por vehículo", description = "Retorna incidentes activos de un vehículo específico")
    @GetMapping("/active/vehicle/{vehicleId}")
    public List<Incident> getActiveIncidentsForVehicle(
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        return incidentRepository.findActiveIncidentsForVehicle(vehicleId, LocalDateTime.now());
    }

    @Operation(summary = "Obtener incidentes resueltos", description = "Retorna todos los incidentes que han sido resueltos")
    @GetMapping("/resolved")
    public List<Incident> getResolvedIncidents() {
        return incidentRepository.findResolvedIncidents(LocalDateTime.now());
    }

    @Operation(summary = "Obtener incidentes por rango de fechas", description = "Retorna incidentes en un rango de fechas específico")
    @GetMapping("/date-range")
    public List<Incident> getIncidentsByDateRange(
            @Parameter(description = "Fecha de inicio (ISO 8601)") @RequestParam String startDate, 
            @Parameter(description = "Fecha de fin (ISO 8601)") @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return incidentRepository.findIncidentsByDateRange(start, end);
    }

    @Operation(summary = "Crear nuevo incidente", description = "Registra un nuevo incidente en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Incidente creado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Incident.class)) })
    })
    @PostMapping
    public Incident createIncident(@RequestBody Incident incident) {
        return incidentRepository.save(incident);
    }

    @Operation(summary = "Eliminar incidente", description = "Elimina un incidente del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Incidente eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @Parameter(description = "ID del incidente a eliminar") @PathVariable String id) {
        if (incidentRepository.existsById(id)) {
            incidentRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

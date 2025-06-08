package com.example.plgsystem.controller;

import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.MaintenanceType;
import com.example.plgsystem.repository.MaintenanceRepository;
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
@RequestMapping("/api/maintenance")
@Tag(name = "Maintenance", description = "Gestión de mantenimientos del sistema PLG")
public class MaintenanceController {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Operation(summary = "Obtener todos los mantenimientos", description = "Retorna la lista completa de mantenimientos registrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de mantenimientos obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Maintenance.class)) })
    })
    @GetMapping
    public List<Maintenance> getAllMaintenance() {
        return maintenanceRepository.findAll();
    }

    @Operation(summary = "Obtener mantenimiento por ID", description = "Retorna un mantenimiento específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mantenimiento encontrado",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Maintenance.class)) }),
        @ApiResponse(responseCode = "404", description = "Mantenimiento no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Maintenance> getMaintenanceById(
            @Parameter(description = "ID del mantenimiento") @PathVariable Long id) {
        Optional<Maintenance> maintenance = maintenanceRepository.findById(id);
        return maintenance.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener mantenimientos por vehículo", description = "Retorna todos los mantenimientos de un vehículo específico")
    @GetMapping("/vehicle/{vehicleId}")
    public List<Maintenance> getMaintenanceByVehicle(
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        return maintenanceRepository.findByVehicleId(vehicleId);
    }

    @Operation(summary = "Obtener mantenimientos por tipo", description = "Retorna mantenimientos filtrados por tipo")
    @GetMapping("/type/{type}")
    public List<Maintenance> getMaintenanceByType(
            @Parameter(description = "Tipo de mantenimiento") @PathVariable MaintenanceType type) {
        return maintenanceRepository.findByType(type);
    }

    @Operation(summary = "Obtener mantenimientos activos", description = "Retorna todos los mantenimientos actualmente en curso")
    @GetMapping("/active")
    public List<Maintenance> getActiveMaintenance() {
        return maintenanceRepository.findActiveMaintenance(LocalDateTime.now());
    }

    @Operation(summary = "Obtener mantenimientos activos por vehículo", description = "Retorna mantenimientos activos de un vehículo específico")
    @GetMapping("/active/vehicle/{vehicleId}")
    public List<Maintenance> getActiveMaintenanceForVehicle(
            @Parameter(description = "ID del vehículo") @PathVariable String vehicleId) {
        return maintenanceRepository.findActiveMaintenanceForVehicle(vehicleId, LocalDateTime.now());
    }

    @Operation(summary = "Obtener mantenimientos próximos", description = "Retorna mantenimientos programados en un rango de fechas")
    @GetMapping("/upcoming")
    public List<Maintenance> getUpcomingMaintenance(
            @Parameter(description = "Fecha de inicio (ISO 8601)") @RequestParam String startDate, 
            @Parameter(description = "Fecha de fin (ISO 8601)") @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return maintenanceRepository.findUpcomingMaintenance(start, end);
    }

    @Operation(summary = "Crear nuevo mantenimiento", description = "Registra un nuevo mantenimiento en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mantenimiento creado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Maintenance.class)) })
    })
    @PostMapping
    public Maintenance createMaintenance(@RequestBody Maintenance maintenance) {
        return maintenanceRepository.save(maintenance);
    }

    @Operation(summary = "Programar mantenimiento", description = "Programa un nuevo mantenimiento para un vehículo")
    @PostMapping("/schedule")
    public Maintenance scheduleMaintenance(
            @Parameter(description = "ID del vehículo") @RequestParam String vehicleId,
            @Parameter(description = "Fecha de inicio (ISO 8601)") @RequestParam String startDate,
            @Parameter(description = "Tipo de mantenimiento") @RequestParam MaintenanceType type) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        Maintenance maintenance = new Maintenance(vehicleId, start, type);
        return maintenanceRepository.save(maintenance);
    }

    @Operation(summary = "Completar mantenimiento", description = "Marca un mantenimiento como completado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mantenimiento completado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Maintenance.class)) }),
        @ApiResponse(responseCode = "404", description = "Mantenimiento no encontrado")
    })
    @PutMapping("/{id}/complete")
    public ResponseEntity<Maintenance> completeMaintenance(
            @Parameter(description = "ID del mantenimiento") @PathVariable Long id) {
        Optional<Maintenance> optionalMaintenance = maintenanceRepository.findById(id);
        if (optionalMaintenance.isPresent()) {
            Maintenance maintenance = optionalMaintenance.get();
            maintenance.setEndDate(LocalDateTime.now());
            Maintenance updatedMaintenance = maintenanceRepository.save(maintenance);
            return ResponseEntity.ok(updatedMaintenance);
        }
        return ResponseEntity.notFound().build();
    }
}

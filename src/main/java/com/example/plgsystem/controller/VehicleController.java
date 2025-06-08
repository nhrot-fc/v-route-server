package com.example.plgsystem.controller;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.VehicleStatus;
import com.example.plgsystem.model.VehicleType;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.VehicleRepository;
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
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Gestión de vehículos del sistema PLG")
public class VehicleController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Operation(summary = "Obtener todos los vehículos", description = "Retorna una lista de todos los vehículos en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de vehículos obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) })
    })
    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Operation(summary = "Obtener vehículo por ID", description = "Retorna un vehículo específico por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vehículo encontrado",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) }),
        @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(
            @Parameter(description = "ID del vehículo") @PathVariable String id) {
        Optional<Vehicle> vehicle = vehicleRepository.findById(id);
        return vehicle.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener vehículos por estado", description = "Retorna vehículos filtrados por estado")
    @GetMapping("/status/{status}")
    public List<Vehicle> getVehiclesByStatus(
            @Parameter(description = "Estado del vehículo") @PathVariable VehicleStatus status) {
        return vehicleRepository.findByStatus(status);
    }

    @Operation(summary = "Obtener vehículos disponibles", description = "Retorna todos los vehículos disponibles")
    @GetMapping("/available")
    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findAvailableVehicles();
    }

    @Operation(summary = "Obtener vehículos por tipo", description = "Retorna vehículos filtrados por tipo")
    @GetMapping("/type/{type}")
    public List<Vehicle> getVehiclesByType(
            @Parameter(description = "Tipo de vehículo") @PathVariable VehicleType type) {
        return vehicleRepository.findByType(type);
    }

    @Operation(summary = "Obtener vehículos por radio", description = "Retorna vehículos dentro de un radio específico")
    @GetMapping("/radius")
    public List<Vehicle> getVehiclesByRadius(
            @Parameter(description = "Coordenada X del centro") @RequestParam double x, 
            @Parameter(description = "Coordenada Y del centro") @RequestParam double y, 
            @Parameter(description = "Radio de búsqueda") @RequestParam double radius) {
        return vehicleRepository.findByRadius(x, y, radius);
    }

    @Operation(summary = "Crear nuevo vehículo", description = "Crea un nuevo vehículo en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vehículo creado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) })
    })
    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(
            @Parameter(description = "Datos del vehículo a crear") @RequestBody Vehicle vehicle) {
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(savedVehicle);
    }

    @Operation(summary = "Eliminar vehículo", description = "Elimina un vehículo del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vehículo eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "ID del vehículo a eliminar") @PathVariable String id) {
        if (vehicleRepository.existsById(id)) {
            vehicleRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Actualizar posición del vehículo", description = "Actualiza la posición actual del vehículo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Posición actualizada exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) }),
        @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @PutMapping("/{id}/position")
    public ResponseEntity<Vehicle> updateVehiclePosition(
            @Parameter(description = "ID del vehículo") @PathVariable String id, 
            @Parameter(description = "Nueva posición") @RequestBody Position position) {
        Optional<Vehicle> optionalVehicle = vehicleRepository.findById(id);
        if (optionalVehicle.isPresent()) {
            Vehicle vehicle = optionalVehicle.get();
            vehicle.setCurrentPosition(position);
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(updatedVehicle);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Actualizar nivel de GLP", description = "Actualiza el nivel de GLP del vehículo (cantidad a agregar/quitar)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nivel de GLP actualizado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) }),
        @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @PutMapping("/{id}/glp")
    public ResponseEntity<Vehicle> updateGLPLevel(
            @Parameter(description = "ID del vehículo") @PathVariable String id, 
            @Parameter(description = "Cantidad de GLP a agregar (positivo) o quitar (negativo)") @RequestParam double amount) {
        Optional<Vehicle> optionalVehicle = vehicleRepository.findById(id);
        if (optionalVehicle.isPresent()) {
            Vehicle vehicle = optionalVehicle.get();
            double newLevel = vehicle.getCurrentGLP() + amount;
            vehicle.setCurrentGLP(Math.max(0, newLevel)); // Ensure it doesn't go negative
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(updatedVehicle);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Actualizar estado del vehículo", description = "Actualiza el estado operacional del vehículo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Vehicle.class)) }),
        @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<Vehicle> updateVehicleStatus(
            @Parameter(description = "ID del vehículo") @PathVariable String id, 
            @Parameter(description = "Nuevo estado del vehículo") @RequestParam VehicleStatus status) {
        Optional<Vehicle> optionalVehicle = vehicleRepository.findById(id);
        if (optionalVehicle.isPresent()) {
            Vehicle vehicle = optionalVehicle.get();
            vehicle.setStatus(status);
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(updatedVehicle);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Obtener capacidad total de la flota", description = "Retorna la capacidad total de GLP de todos los vehículos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Capacidad total obtenida exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Double.class)) })
    })
    @GetMapping("/fleet/capacity")
    public ResponseEntity<Double> getTotalFleetCapacity() {
        Double capacity = vehicleRepository.getTotalFleetCapacity();
        return ResponseEntity.ok(capacity != null ? capacity : 0.0);
    }

    @Operation(summary = "Obtener GLP disponible en la flota", description = "Retorna el total de GLP disponible en vehículos disponibles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GLP disponible obtenido exitosamente",
                    content = { @Content(mediaType = "application/json", 
                               schema = @Schema(implementation = Double.class)) })
    })
    @GetMapping("/fleet/available-glp")
    public ResponseEntity<Double> getAvailableFleetGLP() {
        Double glp = vehicleRepository.getAvailableFleetGLP();
        return ResponseEntity.ok(glp != null ? glp : 0.0);
    }
}

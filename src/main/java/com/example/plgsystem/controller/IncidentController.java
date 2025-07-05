package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.dto.IncidentDTO;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.service.IncidentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Crear un nuevo incidente
     */
    @PostMapping
    public ResponseEntity<IncidentDTO> create(@RequestBody IncidentCreateDTO createDTO) {
        Incident incident = new Incident(
                createDTO.getVehicleId(),
                createDTO.getType(),
                createDTO.getShift()
        );
        
        incident.setOccurrenceTime(createDTO.getOccurrenceTime() != null ? 
                createDTO.getOccurrenceTime() : LocalDateTime.now());
        incident.setLocation(createDTO.getLocation());
        incident.setTransferableGlp(createDTO.getTransferableGlp());
        
        Incident savedIncident = incidentService.save(incident);
        return new ResponseEntity<>(IncidentDTO.fromEntity(savedIncident), HttpStatus.CREATED);
    }

    /**
     * Listar incidentes con opciones de filtrado
     */
    @GetMapping
    public ResponseEntity<List<IncidentDTO>> list(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Incident> incidents;
        
        if (vehicleId != null && startDate != null && endDate != null) {
            incidents = incidentService.findByVehicleAndDateRange(vehicleId, startDate, endDate);
        } else if (vehicleId != null) {
            incidents = incidentService.findByVehicleId(vehicleId);
        } else if (startDate != null && endDate != null) {
            incidents = incidentService.findByDateRange(startDate, endDate);
        } else {
            incidents = incidentService.findAll();
        }
        
        List<IncidentDTO> incidentDTOs = incidents.stream()
                .map(IncidentDTO::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(incidentDTOs);
    }
    
    /**
     * Obtener un incidente por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> getById(@PathVariable Long id) {
        return incidentService.findById(id)
                .map(incident -> ResponseEntity.ok(IncidentDTO.fromEntity(incident)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Marcar un incidente como resuelto
     */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<IncidentDTO> resolveIncident(@PathVariable Long id) {
        return incidentService.resolveIncident(id)
                .map(incident -> ResponseEntity.ok(IncidentDTO.fromEntity(incident)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

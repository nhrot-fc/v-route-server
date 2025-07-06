package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.dto.IncidentDTO;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.service.IncidentService;
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
     * Listar incidentes con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "occurrenceTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Incident> incidents;
            
            if (vehicleId != null && startDate != null && endDate != null) {
                incidents = incidentService.findByVehicleAndDateRange(vehicleId, startDate, endDate);
            } else if (vehicleId != null) {
                incidents = incidentService.findByVehicleId(vehicleId);
            } else if (startDate != null && endDate != null) {
                incidents = incidentService.findByDateRange(startDate, endDate);
            } else if (resolved != null) {
                incidents = incidentService.findByResolved(resolved);
            } else {
                incidents = incidentService.findAll();
            }
            
            List<IncidentDTO> incidentDTOs = incidents.stream()
                    .map(IncidentDTO::fromEntity)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(incidentDTOs);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Incident> incidentPage;
        
        if (vehicleId != null && startDate != null && endDate != null) {
            incidentPage = incidentService.findByVehicleAndDateRangePaged(vehicleId, startDate, endDate, pageable);
        } else if (vehicleId != null) {
            incidentPage = incidentService.findByVehicleIdPaged(vehicleId, pageable);
        } else if (startDate != null && endDate != null) {
            incidentPage = incidentService.findByDateRangePaged(startDate, endDate, pageable);
        } else if (resolved != null) {
            incidentPage = incidentService.findByResolvedPaged(resolved, pageable);
        } else {
            incidentPage = incidentService.findAllPaged(pageable);
        }
        
        return ResponseEntity.ok(incidentPage.map(IncidentDTO::fromEntity));
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

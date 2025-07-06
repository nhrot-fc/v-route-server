package com.example.plgsystem.service;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.repository.IncidentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Guarda un incidente en la base de datos
     */
    @Transactional
    public Incident save(Incident incident) {
        return incidentRepository.save(incident);
    }

    /**
     * Busca un incidente por su ID
     */
    public Optional<Incident> findById(Long id) {
        return incidentRepository.findById(id);
    }

    /**
     * Obtiene todos los incidentes
     */
    public List<Incident> findAll() {
        return incidentRepository.findAll();
    }
    
    /**
     * Obtiene todos los incidentes (paginado)
     */
    public Page<Incident> findAllPaged(Pageable pageable) {
        return incidentRepository.findAll(pageable);
    }
    
    /**
     * Busca incidentes por ID del vehículo
     */
    public List<Incident> findByVehicleId(String vehicleId) {
        return incidentRepository.findByVehicleId(vehicleId);
    }
    
    /**
     * Busca incidentes por ID del vehículo (paginado)
     */
    public Page<Incident> findByVehicleIdPaged(String vehicleId, Pageable pageable) {
        return incidentRepository.findByVehicleId(vehicleId, pageable);
    }
    
    /**
     * Busca incidentes por estado de resolución
     */
    public List<Incident> findByResolved(boolean resolved) {
        return incidentRepository.findByResolved(resolved);
    }
    
    /**
     * Busca incidentes por estado de resolución (paginado)
     */
    public Page<Incident> findByResolvedPaged(boolean resolved, Pageable pageable) {
        return incidentRepository.findByResolved(resolved, pageable);
    }
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas
     */
    public List<Incident> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return incidentRepository.findByOccurrenceTimeBetween(start, end);
    }
    
    /**
     * Busca incidentes ocurridos dentro de un rango de fechas (paginado)
     */
    public Page<Incident> findByDateRangePaged(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return incidentRepository.findByOccurrenceTimeBetween(start, end, pageable);
    }
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas
     */
    public List<Incident> findByVehicleAndDateRange(String vehicleId, LocalDateTime start, LocalDateTime end) {
        return incidentRepository.findByVehicleIdAndOccurrenceTimeBetween(vehicleId, start, end);
    }
    
    /**
     * Busca incidentes para un vehículo específico dentro de un rango de fechas (paginado)
     */
    public Page<Incident> findByVehicleAndDateRangePaged(String vehicleId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return incidentRepository.findByVehicleIdAndOccurrenceTimeBetween(vehicleId, start, end, pageable);
    }
    
    /**
     * Marca un incidente como resuelto
     */
    @Transactional
    public Optional<Incident> resolveIncident(Long id) {
        return findById(id)
                .map(incident -> {
                    incident.setResolved(true);
                    return incidentRepository.save(incident);
                });
    }
}

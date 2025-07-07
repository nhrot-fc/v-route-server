package com.example.plgsystem.service;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.repository.DepotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DepotService {

    private final DepotRepository depotRepository;

    public DepotService(DepotRepository depotRepository) {
        this.depotRepository = depotRepository;
    }

    /**
     * Guarda un depósito en la base de datos (crear o actualizar)
     */
    @Transactional
    public Depot save(Depot depot) {
        return depotRepository.save(depot);
    }

    /**
     * Busca un depósito por su ID
     */
    public Optional<Depot> findById(String id) {
        return depotRepository.findById(id);
    }

    /**
     * Obtiene todos los depósitos
     */
    public List<Depot> findAll() {
        return depotRepository.findAll();
    }
    
    /**
     * Obtiene todos los depósitos (paginado)
     */
    public Page<Depot> findAllPaged(Pageable pageable) {
        return depotRepository.findAll(pageable);
    }

    /**
     * Elimina un depósito por ID
     */
    @Transactional
    public void deleteById(String id) {
        depotRepository.deleteById(id);
    }
    
    /**
     * Busca depósitos por tipo
     */
    public List<Depot> findByType(DepotType type) {
        return depotRepository.findByType(type);
    }
    
    /**
     * Busca depósitos por tipo (paginado)
     */
    public Page<Depot> findByTypePaged(DepotType type, Pageable pageable) {
        return depotRepository.findByType(type, pageable);
    }
    
    /**
     * Busca depósitos principales
     */
    public List<Depot> findMainDepots() {
        return depotRepository.findMainDepots();
    }
    
    /**
     * Busca depósitos auxiliares
     */
    public List<Depot> findAuxiliaryDepots() {
        return depotRepository.findAuxiliaryDepots();
    }
    
    /**
     * Filtra depósitos por capacidad mínima
     */
    public List<Depot> findByMinCapacity(Integer minCapacity) {
        return depotRepository.findByGlpCapacityM3GreaterThanEqual(minCapacity);
    }
    
    /**
     * Filtra depósitos por capacidad mínima (paginado)
     */
    public Page<Depot> findByMinCapacityPaged(Integer minCapacity, Pageable pageable) {
        return depotRepository.findByGlpCapacityM3GreaterThanEqual(minCapacity, pageable);
    }
    
    /**
     * Filtra depósitos por GLP disponible mínimo
     */
    public List<Depot> findByMinCurrentGlp(Integer minCurrentGlp) {
        return depotRepository.findByCurrentGlpM3GreaterThanEqual(minCurrentGlp);
    }
    
    /**
     * Filtra depósitos por GLP disponible mínimo (paginado)
     */
    public Page<Depot> findByMinCurrentGlpPaged(Integer minCurrentGlp, Pageable pageable) {
        return depotRepository.findByCurrentGlpM3GreaterThanEqual(minCurrentGlp, pageable);
    }
    
    /**
     * Encuentra el depósito más cercano a una posición que tenga suficiente GLP disponible
     * 
     * @param position Posición de referencia
     * @param glpNeededM3 Cantidad de GLP requerida
     * @return Depósito más cercano que puede satisfacer la necesidad
     */
    public Optional<Depot> findNearestDepotWithGLP(Position position, int glpNeededM3) {
        List<Depot> candidates = depotRepository.findByCurrentGlpM3GreaterThanEqual(glpNeededM3);
        
        return candidates.stream()
                .min(Comparator.comparing(depot -> depot.getPosition().distanceTo(position)));
    }
    
    /**
     * Rellena el GLP de un depósito
     * 
     * @param depotId ID del depósito
     * @return El depósito actualizado o empty si no existe
     */
    @Transactional
    public Optional<Depot> refillDepotGLP(String depotId) {
        return depotRepository.findById(depotId)
                .map(depot -> {
                    depot.refill();
                    return depotRepository.save(depot);
                });
    }
    
    /**
     * Sirve una cantidad de GLP desde un depósito
     * 
     * @param depotId ID del depósito
     * @param amountM3 Cantidad de GLP a servir
     * @return El depósito actualizado o empty si no existe o no tiene suficiente GLP
     */
    @Transactional
    public Optional<Depot> serveGLPFromDepot(String depotId, int amountM3) {
        return depotRepository.findById(depotId)
                .filter(depot -> depot.canServe(amountM3))
                .map(depot -> {
                    depot.serve(amountM3);
                    return depotRepository.save(depot);
                });
    }
}

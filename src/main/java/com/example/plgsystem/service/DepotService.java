package com.example.plgsystem.service;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.DepotRepository;
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
     * Elimina un depósito por ID
     */
    @Transactional
    public void deleteById(String id) {
        depotRepository.deleteById(id);
    }
    
    /**
     * Filtra depósitos por capacidad de recarga
     */
    public List<Depot> findByCanRefuel(Boolean canRefuel) {
        return depotRepository.findByCanRefuel(canRefuel);
    }
    
    /**
     * Filtra depósitos por capacidad mínima
     */
    public List<Depot> findByMinCapacity(Integer minCapacity) {
        return depotRepository.findByGlpCapacityM3GreaterThanEqual(minCapacity);
    }
    
    /**
     * Filtra depósitos por GLP disponible mínimo
     */
    public List<Depot> findByMinCurrentGlp(Integer minCurrentGlp) {
        return depotRepository.findByCurrentGlpM3GreaterThanEqual(minCurrentGlp);
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
                    depot.refillGLP();
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
                .filter(depot -> depot.canServeGLP(amountM3))
                .map(depot -> {
                    depot.serveGLP(amountM3);
                    return depotRepository.save(depot);
                });
    }
    
    /**
     * Elimina un depósito por su ID
     */
    @Transactional
    public void deleteDepot(String id) {
        depotRepository.deleteById(id);
    }
}

package com.example.plgsystem.service;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final OrderService orderService;

    public VehicleService(VehicleRepository vehicleRepository, OrderService orderService) {
        this.vehicleRepository = vehicleRepository;
        this.orderService = orderService;
    }

    /**
     * Guarda un vehículo en la base de datos
     */
    @Transactional
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    /**
     * Busca un vehículo por su ID
     */
    public Optional<Vehicle> findById(String id) {
        return vehicleRepository.findById(id);
    }

    /**
     * Obtiene todos los vehículos
     */
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }
    
    /**
     * Obtiene todos los vehículos (paginado)
     */
    public Page<Vehicle> findAllPaged(Pageable pageable) {
        return vehicleRepository.findAll(pageable);
    }

    /**
     * Elimina un vehículo por ID
     */
    @Transactional
    public void deleteById(String id) {
        vehicleRepository.deleteById(id);
    }
    
    /**
     * Busca vehículos por tipo
     */
    public List<Vehicle> findByType(VehicleType type) {
        return vehicleRepository.findByType(type);
    }
    
    /**
     * Busca vehículos por tipo (paginado)
     */
    public Page<Vehicle> findByTypePaged(VehicleType type, Pageable pageable) {
        return vehicleRepository.findByType(type, pageable);
    }
    
    /**
     * Busca vehículos por estado
     */
    public List<Vehicle> findByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status);
    }
    
    /**
     * Busca vehículos por estado (paginado)
     */
    public Page<Vehicle> findByStatusPaged(VehicleStatus status, Pageable pageable) {
        return vehicleRepository.findByStatus(status, pageable);
    }
    
    /**
     * Busca vehículos disponibles ordenados por capacidad de GLP descendente
     */
    public List<Vehicle> findAvailableVehiclesOrderByGlp() {
        return vehicleRepository.findByStatusOrderByCurrentGlpM3Desc(VehicleStatus.AVAILABLE);
    }
    
    /**
     * Busca vehículos disponibles ordenados por capacidad de GLP descendente (paginado)
     */
    public Page<Vehicle> findAvailableVehiclesOrderByGlpPaged(Pageable pageable) {
        return vehicleRepository.findByStatusOrderByCurrentGlpM3Desc(VehicleStatus.AVAILABLE, pageable);
    }
    
    /**
     * Busca vehículos con capacidad mínima de GLP
     */
    public List<Vehicle> findByMinimumGlp(int minGlp) {
        return vehicleRepository.findByCurrentGlpM3GreaterThanEqual(minGlp);
    }
    
    /**
     * Busca vehículos con capacidad mínima de GLP (paginado)
     */
    public Page<Vehicle> findByMinimumGlpPaged(int minGlp, Pageable pageable) {
        return vehicleRepository.findByCurrentGlpM3GreaterThanEqual(minGlp, pageable);
    }
    
    /**
     * Busca vehículos con capacidad mínima de combustible
     */
    public List<Vehicle> findByMinimumFuel(double minFuel) {
        return vehicleRepository.findByCurrentFuelGalGreaterThanEqual(minFuel);
    }
    
    /**
     * Busca vehículos con capacidad mínima de combustible (paginado)
     */
    public Page<Vehicle> findByMinimumFuelPaged(double minFuel, Pageable pageable) {
        return vehicleRepository.findByCurrentFuelGalGreaterThanEqual(minFuel, pageable);
    }
    
    /**
     * Realiza el reabastecimiento de combustible de un vehículo
     */
    @Transactional
    public Optional<Vehicle> refuelVehicle(String id) {
        return findById(id).map(vehicle -> {
            vehicle.refuel();
            return vehicleRepository.save(vehicle);
        });
    }
    
    /**
     * Realiza la recarga de GLP de un vehículo
     */
    @Transactional
    public Optional<Vehicle> refillGlp(String id, int glpVolumeM3) {
        return findById(id).map(vehicle -> {
            vehicle.refill(glpVolumeM3);
            return vehicleRepository.save(vehicle);
        });
    }
    
    /**
     * Dispensa GLP de un vehículo
     */
    @Transactional
    public Optional<Vehicle> dispenseGlp(String id, int glpVolumeM3) {
        return findById(id).map(vehicle -> {
            if (vehicle.canDispense(glpVolumeM3)) {
                vehicle.dispense(glpVolumeM3);
                return vehicleRepository.save(vehicle);
            }
            return vehicle;
        });
    }
    
    /**
     * Realiza la entrega de un pedido
     */
    @Transactional
    public Optional<ServeRecord> serveOrder(String vehicleId, String orderId, int glpVolumeM3, LocalDateTime serveDate) {
        Optional<Vehicle> optionalVehicle = findById(vehicleId);
        Optional<Order> optionalOrder = orderService.findById(orderId);
        
        if (optionalVehicle.isPresent() && optionalOrder.isPresent()) {
            Vehicle vehicle = optionalVehicle.get();
            Order order = optionalOrder.get();
            
            if (vehicle.canDispense(glpVolumeM3)) {
                LocalDateTime deliveryTime = serveDate != null ? serveDate : LocalDateTime.now();
                ServeRecord record = vehicle.serveOrder(order, glpVolumeM3, deliveryTime);
                vehicleRepository.save(vehicle);
                orderService.save(order);
                return Optional.of(record);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Actualiza la posición de un vehículo y consume combustible según la distancia
     */
    @Transactional
    public Optional<Vehicle> moveVehicle(String id, double distanceKm) {
        return findById(id).map(vehicle -> {
            vehicle.consumeFuel(distanceKm);
            return vehicleRepository.save(vehicle);
        });
    }

    public Vehicle findVehicleById(String id) {
        Optional<Vehicle> vehicle = vehicleRepository.findById(id);
        return vehicle.orElse(null);
    }
}

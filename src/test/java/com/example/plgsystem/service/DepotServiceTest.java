package com.example.plgsystem.service;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.repository.DepotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DepotServiceTest {

    @InjectMocks
    private DepotService depotService;

    @Mock
    private DepotRepository depotRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_shouldReturnSavedDepot() {
        // Arrange
        Position position = new Position(10, 10);
        Depot depot = new Depot("D-001", position, 1000, DepotType.MAIN);
        when(depotRepository.save(any(Depot.class))).thenReturn(depot);
        
        // Act
        Depot savedDepot = depotService.save(depot);
        
        // Assert
        assertEquals("D-001", savedDepot.getId());
        assertEquals(DepotType.MAIN, savedDepot.getType());
        assertEquals(10, savedDepot.getPosition().getX());
        assertEquals(10, savedDepot.getPosition().getY());
        assertEquals(1000, savedDepot.getGlpCapacityM3());
        verify(depotRepository, times(1)).save(depot);
    }

    @Test
    void findById_shouldReturnDepotWhenExists() {
        // Arrange
        Position position = new Position(10, 10);
        Depot depot = new Depot("D-001", position, 1000, DepotType.MAIN);
        when(depotRepository.findById("D-001")).thenReturn(Optional.of(depot));
        
        // Act
        Optional<Depot> result = depotService.findById("D-001");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("D-001", result.get().getId());
    }
    
    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // Arrange
        when(depotRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        // Act
        Optional<Depot> result = depotService.findById("non-existent");
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void findAll_shouldReturnAllDepots() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(20, 20);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        Depot depot2 = new Depot("D-002", position2, 500, DepotType.AUXILIARY);
        
        List<Depot> depots = new ArrayList<>();
        depots.add(depot1);
        depots.add(depot2);
        
        when(depotRepository.findAll()).thenReturn(depots);
        
        // Act
        List<Depot> result = depotService.findAll();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("D-001", result.get(0).getId());
        assertEquals("D-002", result.get(1).getId());
    }
    
    @Test
    void findAllPaged_shouldReturnPagedDepots() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(20, 20);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        Depot depot2 = new Depot("D-002", position2, 500, DepotType.AUXILIARY);
        
        List<Depot> depots = new ArrayList<>();
        depots.add(depot1);
        depots.add(depot2);
        
        Page<Depot> pagedDepots = new PageImpl<>(depots);
        Pageable pageable = PageRequest.of(0, 10);
        when(depotRepository.findAll(pageable)).thenReturn(pagedDepots);
        
        // Act
        Page<Depot> result = depotService.findAllPaged(pageable);
        
        // Assert
        assertEquals(2, result.getContent().size());
    }
    
    @Test
    void deleteById_shouldCallRepositoryDelete() {
        // Arrange
        String depotId = "D-001";
        doNothing().when(depotRepository).deleteById(depotId);
        
        // Act
        depotService.deleteById(depotId);
        
        // Assert
        verify(depotRepository, times(1)).deleteById(depotId);
    }
    
    @Test
    void findByType_shouldReturnDepotsOfType() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(20, 20);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        Depot depot2 = new Depot("D-002", position2, 1200, DepotType.MAIN);
        
        List<Depot> mainDepots = new ArrayList<>();
        mainDepots.add(depot1);
        mainDepots.add(depot2);
        
        when(depotRepository.findByType(DepotType.MAIN)).thenReturn(mainDepots);
        
        // Act
        List<Depot> result = depotService.findByType(DepotType.MAIN);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(DepotType.MAIN, result.get(0).getType());
        assertEquals(DepotType.MAIN, result.get(1).getType());
    }
    
    @Test
    void findMainDepots_shouldReturnMainDepots() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(20, 20);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        Depot depot2 = new Depot("D-002", position2, 1200, DepotType.MAIN);
        
        List<Depot> mainDepots = new ArrayList<>();
        mainDepots.add(depot1);
        mainDepots.add(depot2);
        
        when(depotRepository.findMainDepots()).thenReturn(mainDepots);
        
        // Act
        List<Depot> result = depotService.findMainDepots();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(DepotType.MAIN, result.get(0).getType());
        assertEquals(DepotType.MAIN, result.get(1).getType());
    }
    
    @Test
    void findAuxiliaryDepots_shouldReturnAuxiliaryDepots() {
        // Arrange
        Position position1 = new Position(30, 30);
        Position position2 = new Position(40, 40);
        
        Depot depot1 = new Depot("D-003", position1, 500, DepotType.AUXILIARY);
        Depot depot2 = new Depot("D-004", position2, 600, DepotType.AUXILIARY);
        
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(depot1);
        auxDepots.add(depot2);
        
        when(depotRepository.findAuxiliaryDepots()).thenReturn(auxDepots);
        
        // Act
        List<Depot> result = depotService.findAuxiliaryDepots();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(DepotType.AUXILIARY, result.get(0).getType());
        assertEquals(DepotType.AUXILIARY, result.get(1).getType());
    }
    
    @Test
    void findByMinCapacity_shouldReturnDepotsWithMinCapacity() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(30, 30);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        Depot depot2 = new Depot("D-003", position2, 500, DepotType.AUXILIARY);
        
        List<Depot> depotsWithMinCapacity = new ArrayList<>();
        depotsWithMinCapacity.add(depot1);
        depotsWithMinCapacity.add(depot2);
        
        when(depotRepository.findByGlpCapacityM3GreaterThanEqual(500)).thenReturn(depotsWithMinCapacity);
        
        // Act
        List<Depot> result = depotService.findByMinCapacity(500);
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).getGlpCapacityM3() >= 500);
        assertTrue(result.get(1).getGlpCapacityM3() >= 500);
    }
    
    @Test
    void findNearestDepotWithGLP_shouldReturnNearestDepotWithEnoughGLP() {
        // Arrange
        Position position1 = new Position(10, 10);
        Position position2 = new Position(30, 30);
        
        Depot depot1 = new Depot("D-001", position1, 1000, DepotType.MAIN);
        depot1.setCurrentGlpM3(800);
        
        Depot depot2 = new Depot("D-003", position2, 500, DepotType.AUXILIARY);
        depot2.setCurrentGlpM3(400);
        
        List<Depot> depotsWithEnoughGLP = new ArrayList<>();
        depotsWithEnoughGLP.add(depot1);
        depotsWithEnoughGLP.add(depot2);
        
        Position currentPosition = new Position(15, 15);
        
        when(depotRepository.findByCurrentGlpM3GreaterThanEqual(300)).thenReturn(depotsWithEnoughGLP);
        
        // Act
        Optional<Depot> result = depotService.findNearestDepotWithGLP(currentPosition, 300);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("D-001", result.get().getId()); // Depot1 is closer to position (15,15)
    }
    
    @Test
    void refillDepotGLP_shouldRefillToCapacity() {
        // Arrange
        Position position = new Position(10, 10);
        Depot depot = new Depot("D-001", position, 1000, DepotType.MAIN);
        depot.setCurrentGlpM3(200); // 20% full
        
        when(depotRepository.findById("D-001")).thenReturn(Optional.of(depot));
        when(depotRepository.save(any(Depot.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // Act
        Optional<Depot> result = depotService.refillDepotGLP("D-001");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(1000, result.get().getCurrentGlpM3()); // Should be full (capacity)
        verify(depotRepository, times(1)).save(depot);
    }
    
    @Test
    void serveGLPFromDepot_shouldReduceGLPLevel() {
        // Arrange
        Position position = new Position(10, 10);
        Depot depot = new Depot("D-001", position, 1000, DepotType.AUXILIARY); // AUXILIARY to allow serving
        depot.setCurrentGlpM3(800); // 80% full
        
        when(depotRepository.findById("D-001")).thenReturn(Optional.of(depot));
        when(depotRepository.save(any(Depot.class))).thenAnswer(i -> i.getArguments()[0]);
        
        int amountToServe = 300;
        
        // Act
        Optional<Depot> result = depotService.serveGLPFromDepot("D-001", amountToServe);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(500, result.get().getCurrentGlpM3()); // 800 - 300 = 500
        verify(depotRepository, times(1)).save(depot);
    }
} 
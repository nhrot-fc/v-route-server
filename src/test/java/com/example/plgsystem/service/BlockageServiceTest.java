package com.example.plgsystem.service;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.BlockageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BlockageServiceTest {

    @InjectMocks
    private BlockageService blockageService;

    @Mock
    private BlockageRepository blockageRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_shouldReturnSavedBlockage() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);
        
        List<Position> points = new ArrayList<>();
        points.add(new Position(10, 10));
        points.add(new Position(20, 10));
        points.add(new Position(20, 20));
        points.add(new Position(10, 20));
        
        Blockage blockage = new Blockage(startTime, endTime, points);
        
        when(blockageRepository.save(any(Blockage.class))).thenReturn(blockage);
        
        // Act
        Blockage savedBlockage = blockageService.save(blockage);
        
        // Assert
        assertEquals(startTime, savedBlockage.getStartTime());
        assertEquals(endTime, savedBlockage.getEndTime());
        assertEquals(4, savedBlockage.getLines().size());
        verify(blockageRepository, times(1)).save(blockage);
    }

    @Test
    void findById_shouldReturnBlockageWhenExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);
        
        List<Position> points = new ArrayList<>();
        points.add(new Position(10, 10));
        points.add(new Position(20, 10));
        points.add(new Position(20, 20));
        points.add(new Position(10, 20));
        
        Blockage blockage = new Blockage(startTime, endTime, points);
        
        when(blockageRepository.findById(id)).thenReturn(Optional.of(blockage));
        
        // Act
        Optional<Blockage> result = blockageService.findById(id);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(startTime, result.get().getStartTime());
        assertEquals(endTime, result.get().getEndTime());
    }

    @Test
    void findAll_shouldReturnAllBlockages() {
        // Arrange
        LocalDateTime startTime1 = LocalDateTime.now();
        LocalDateTime endTime1 = startTime1.plusHours(2);
        
        List<Position> points1 = new ArrayList<>();
        points1.add(new Position(10, 10));
        points1.add(new Position(20, 10));
        points1.add(new Position(20, 20));
        points1.add(new Position(10, 20));
        
        Blockage blockage1 = new Blockage(startTime1, endTime1, points1);
        
        LocalDateTime startTime2 = LocalDateTime.now().plusHours(3);
        LocalDateTime endTime2 = startTime2.plusHours(2);
        
        List<Position> points2 = new ArrayList<>();
        points2.add(new Position(30, 30));
        points2.add(new Position(40, 30));
        points2.add(new Position(40, 40));
        points2.add(new Position(30, 40));
        
        Blockage blockage2 = new Blockage(startTime2, endTime2, points2);
        
        List<Blockage> blockages = new ArrayList<>();
        blockages.add(blockage1);
        blockages.add(blockage2);
        
        when(blockageRepository.findAll()).thenReturn(blockages);
        
        // Act
        List<Blockage> result = blockageService.findAll();
        
        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void findAllPaged_shouldReturnPagedBlockages() {
        // Arrange
        LocalDateTime startTime1 = LocalDateTime.now();
        LocalDateTime endTime1 = startTime1.plusHours(2);
        
        List<Position> points1 = new ArrayList<>();
        points1.add(new Position(10, 10));
        points1.add(new Position(20, 10));
        points1.add(new Position(20, 20));
        points1.add(new Position(10, 20));
        
        Blockage blockage1 = new Blockage(startTime1, endTime1, points1);
        
        LocalDateTime startTime2 = LocalDateTime.now().plusHours(3);
        LocalDateTime endTime2 = startTime2.plusHours(2);
        
        List<Position> points2 = new ArrayList<>();
        points2.add(new Position(30, 30));
        points2.add(new Position(40, 30));
        points2.add(new Position(40, 40));
        points2.add(new Position(30, 40));
        
        Blockage blockage2 = new Blockage(startTime2, endTime2, points2);
        
        List<Blockage> blockages = new ArrayList<>();
        blockages.add(blockage1);
        blockages.add(blockage2);
        
        Page<Blockage> pagedBlockages = new PageImpl<>(blockages);
        Pageable pageable = PageRequest.of(0, 10);
        when(blockageRepository.findAll(pageable)).thenReturn(pagedBlockages);
        
        // Act
        Page<Blockage> result = blockageService.findAllPaged(pageable);
        
        // Assert
        assertEquals(2, result.getContent().size());
    }

    @Test
    void findByActiveAtDateTime_shouldReturnActiveBlockages() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime1 = now.minusHours(1);
        LocalDateTime endTime1 = now.plusHours(1);
        
        List<Position> points1 = new ArrayList<>();
        points1.add(new Position(10, 10));
        points1.add(new Position(20, 10));
        points1.add(new Position(20, 20));
        points1.add(new Position(10, 20));
        
        Blockage blockage1 = new Blockage(startTime1, endTime1, points1);
        
        List<Blockage> activeBlockages = new ArrayList<>();
        activeBlockages.add(blockage1);
        
        when(blockageRepository.findByActiveAtDateTime(now)).thenReturn(activeBlockages);
        
        // Act
        List<Blockage> result = blockageService.findByActiveAtDateTime(now);
        
        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void findByTimeRange_shouldReturnBlockagesInRange() {
        // Arrange
        LocalDateTime startRange = LocalDateTime.now();
        LocalDateTime endRange = startRange.plusHours(5);
        
        LocalDateTime startTime1 = startRange.plusHours(1);
        LocalDateTime endTime1 = startTime1.plusHours(2);
        
        List<Position> points1 = new ArrayList<>();
        points1.add(new Position(10, 10));
        points1.add(new Position(20, 10));
        points1.add(new Position(20, 20));
        points1.add(new Position(10, 20));
        
        Blockage blockage1 = new Blockage(startTime1, endTime1, points1);
        
        List<Blockage> blockages = new ArrayList<>();
        blockages.add(blockage1);
        
        when(blockageRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startRange, endRange))
                .thenReturn(blockages);
        
        // Act
        List<Blockage> result = blockageService.findByTimeRange(startRange, endRange);
        
        // Assert
        assertEquals(1, result.size());
    }
} 
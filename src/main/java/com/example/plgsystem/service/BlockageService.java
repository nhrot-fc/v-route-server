package com.example.plgsystem.service;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.repository.BlockageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar bloqueos
 */
@Service
public class BlockageService {

    private final BlockageRepository blockageRepository;
    
    public BlockageService(BlockageRepository blockageRepository) {
        this.blockageRepository = blockageRepository;
    }

    @Transactional
    public Blockage save(Blockage blockage) {
        return blockageRepository.save(blockage);
    }

    public Optional<Blockage> findById(UUID id) {
        return blockageRepository.findById(id);
    }

    @Transactional
    public void deleteById(UUID id) {
        blockageRepository.deleteById(id);
    }

    public List<Blockage> findAll() {
        return blockageRepository.findAll();
    }
    
    public Page<Blockage> findAllPaged(Pageable pageable) {
        return blockageRepository.findAll(pageable);
    }

    public List<Blockage> findByActiveAtDateTime(LocalDateTime dateTime) {
        return blockageRepository.findByActiveAtDateTime(dateTime);
    }
    
    public Page<Blockage> findByActiveAtDateTimePaged(LocalDateTime dateTime, Pageable pageable) {
        return blockageRepository.findByActiveAtDateTime(dateTime, pageable);
    }

    public List<Blockage> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return blockageRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime, endTime);
    }
    
    public Page<Blockage> findByTimeRangePaged(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return blockageRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime, endTime, pageable);
    }
}

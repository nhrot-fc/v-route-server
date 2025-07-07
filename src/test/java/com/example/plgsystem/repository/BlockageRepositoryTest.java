package com.example.plgsystem.repository;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class BlockageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BlockageRepository blockageRepository;

    @Test
    public void testSaveBlockage() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 18, 0);
        List<Position> lines = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10),
                new Position(20, 20),
                new Position(10, 20)
        );
        Blockage blockage = new Blockage(start, end, lines);

        // When
        Blockage savedBlockage = blockageRepository.save(blockage);

        // Then
        assertNotNull(savedBlockage);
        assertNotNull(savedBlockage.getId()); // ID should be generated
        assertEquals(start, savedBlockage.getStartTime());
        assertEquals(end, savedBlockage.getEndTime());
        assertEquals(4, savedBlockage.getLines().size());
    }

    @Test
    public void testFindById() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 18, 0);
        List<Position> lines = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );
        Blockage blockage = new Blockage(start, end, lines);
        entityManager.persist(blockage);
        entityManager.flush();

        UUID blockageId = blockage.getId();

        // When
        Optional<Blockage> found = blockageRepository.findById(blockageId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(blockageId, found.get().getId());
        assertEquals(start, found.get().getStartTime());
        assertEquals(end, found.get().getEndTime());
        assertEquals(2, found.get().getLines().size());
    }

    @Test
    public void testFindAll() {
        // Given
        LocalDateTime start1 = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end1 = LocalDateTime.of(2025, 1, 1, 18, 0);
        List<Position> lines1 = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );

        LocalDateTime start2 = LocalDateTime.of(2025, 2, 1, 10, 0);
        LocalDateTime end2 = LocalDateTime.of(2025, 2, 1, 18, 0);
        List<Position> lines2 = Arrays.asList(
                new Position(30, 30),
                new Position(40, 30)
        );

        Blockage blockage1 = new Blockage(start1, end1, lines1);
        Blockage blockage2 = new Blockage(start2, end2, lines2);

        entityManager.persist(blockage1);
        entityManager.persist(blockage2);
        entityManager.flush();

        // When
        List<Blockage> blockages = blockageRepository.findAll();

        // Then
        assertEquals(2, blockages.size());
    }

    @Test
    public void testUpdateBlockage() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 18, 0);
        List<Position> lines = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );
        Blockage blockage = new Blockage(start, end, lines);
        entityManager.persist(blockage);
        entityManager.flush();

        UUID blockageId = blockage.getId();
        
        // When - Create a new blockage with updated times
        LocalDateTime newEnd = LocalDateTime.of(2025, 1, 1, 20, 0); // Extended end time
        Blockage updatedBlockage = new Blockage(start, newEnd, lines);
        updatedBlockage.setId(blockageId);
        blockageRepository.save(updatedBlockage);
        
        // Then
        Blockage refreshedBlockage = blockageRepository.findById(blockageId).get();
        assertEquals(newEnd, refreshedBlockage.getEndTime());
        assertEquals(start, refreshedBlockage.getStartTime()); // Should not change
    }

    @Test
    public void testDeleteBlockage() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 18, 0);
        List<Position> lines = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );
        Blockage blockage = new Blockage(start, end, lines);
        entityManager.persist(blockage);
        entityManager.flush();

        UUID blockageId = blockage.getId();

        // When
        blockageRepository.deleteById(blockageId);

        // Then
        Optional<Blockage> deleted = blockageRepository.findById(blockageId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindByActiveAtDateTime() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Active blockage (now is between start and end)
        LocalDateTime start1 = now.minusHours(1);
        LocalDateTime end1 = now.plusHours(1);
        List<Position> lines1 = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );
        
        // Not active blockage (already ended)
        LocalDateTime start2 = now.minusHours(3);
        LocalDateTime end2 = now.minusHours(2);
        List<Position> lines2 = Arrays.asList(
                new Position(30, 30),
                new Position(40, 30)
        );
        
        // Not active blockage (not started yet)
        LocalDateTime start3 = now.plusHours(1);
        LocalDateTime end3 = now.plusHours(2);
        List<Position> lines3 = Arrays.asList(
                new Position(50, 50),
                new Position(60, 50)
        );

        Blockage blockage1 = new Blockage(start1, end1, lines1);
        Blockage blockage2 = new Blockage(start2, end2, lines2);
        Blockage blockage3 = new Blockage(start3, end3, lines3);

        entityManager.persist(blockage1);
        entityManager.persist(blockage2);
        entityManager.persist(blockage3);
        entityManager.flush();

        // When
        List<Blockage> activeBlockages = blockageRepository.findByActiveAtDateTime(now);

        // Then
        assertEquals(1, activeBlockages.size());
        assertEquals(blockage1.getId(), activeBlockages.get(0).getId());
    }

    @Test
    public void testFindByStartTimeGreaterThanEqualAndEndTimeLessThanEqual() {
        // Given
        LocalDateTime jan1 = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime feb1 = LocalDateTime.of(2025, 2, 1, 0, 0);
        LocalDateTime mar1 = LocalDateTime.of(2025, 3, 1, 0, 0);
        
        // Blockage in January
        Blockage blockage1 = new Blockage(jan1, jan1.plusDays(5), Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        ));
        
        // Blockage in February
        Blockage blockage2 = new Blockage(feb1, feb1.plusDays(5), Arrays.asList(
                new Position(30, 30),
                new Position(40, 30)
        ));
        
        // Blockage in March
        Blockage blockage3 = new Blockage(mar1, mar1.plusDays(5), Arrays.asList(
                new Position(50, 50),
                new Position(60, 50)
        ));

        entityManager.persist(blockage1);
        entityManager.persist(blockage2);
        entityManager.persist(blockage3);
        entityManager.flush();

        // When - Find blockages in February and March
        List<Blockage> q1Blockages = blockageRepository
                .findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(feb1, mar1.plusDays(10));

        // Then
        assertEquals(2, q1Blockages.size());
        assertFalse(q1Blockages.stream().anyMatch(b -> b.getStartTime().equals(jan1)));
    }
    
    @Test
    public void testIsActiveAt() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Active blockage
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.plusHours(1);
        List<Position> lines = Arrays.asList(
                new Position(10, 10),
                new Position(20, 10)
        );
        
        Blockage blockage = new Blockage(start, end, lines);
        
        // When & Then
        assertTrue(blockage.isActiveAt(now));
        assertTrue(blockage.isActiveAt(start)); // Start time is inclusive
        assertTrue(blockage.isActiveAt(end));   // End time is inclusive
        assertFalse(blockage.isActiveAt(start.minusSeconds(1))); // Just before start
        assertFalse(blockage.isActiveAt(end.plusSeconds(1)));    // Just after end
    }
} 
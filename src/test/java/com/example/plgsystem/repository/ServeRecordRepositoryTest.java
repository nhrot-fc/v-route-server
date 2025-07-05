package com.example.plgsystem.repository;

import com.example.plgsystem.model.ServeRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ServeRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ServeRecordRepository serveRecordRepository;

    @Test
    public void testSaveServeRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record = new ServeRecord("V001", "O001", 50, now);

        // When
        ServeRecord savedRecord = serveRecordRepository.save(record);

        // Then
        assertNotNull(savedRecord);
        assertNotNull(savedRecord.getId()); // ID should be generated
        assertEquals("V001", savedRecord.getVehicleId());
        assertEquals("O001", savedRecord.getOrderId());
        assertEquals(50, savedRecord.getVolumeM3());
        assertEquals(now, savedRecord.getServeDate());
    }

    @Test
    public void testFindById() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record = new ServeRecord("V001", "O001", 50, now);
        entityManager.persist(record);
        entityManager.flush();

        Long recordId = record.getId();

        // When
        Optional<ServeRecord> found = serveRecordRepository.findById(recordId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(recordId, found.get().getId());
        assertEquals("V001", found.get().getVehicleId());
        assertEquals("O001", found.get().getOrderId());
        assertEquals(50, found.get().getVolumeM3());
    }

    @Test
    public void testFindAll() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, now);
        ServeRecord record2 = new ServeRecord("V002", "O002", 75, now.plusHours(1));

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.flush();

        // When
        List<ServeRecord> records = serveRecordRepository.findAll();

        // Then
        assertEquals(2, records.size());
    }

    @Test
    public void testUpdateServeRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record = new ServeRecord("V001", "O001", 50, now);
        entityManager.persist(record);
        entityManager.flush();

        Long recordId = record.getId();

        // When - Note: In real scenario, you normally wouldn't update serve records
        // This test is for completeness of CRUD operations
        ServeRecord savedRecord = serveRecordRepository.findById(recordId).get();
        
        // Since ServeRecord doesn't have setters, we need to create a new record and update it
        ServeRecord updatedRecord = new ServeRecord(savedRecord.getVehicleId(), savedRecord.getOrderId(), 75, savedRecord.getServeDate());
        // We can't set the ID directly, so we need to delete the old record and save the new one
        serveRecordRepository.deleteById(recordId);
        ServeRecord newRecord = serveRecordRepository.save(updatedRecord);

        // Then
        assertNotNull(newRecord.getId());
        assertEquals(75, newRecord.getVolumeM3());
        assertEquals("V001", newRecord.getVehicleId());
    }

    @Test
    public void testDeleteServeRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record = new ServeRecord("V001", "O001", 50, now);
        entityManager.persist(record);
        entityManager.flush();

        Long recordId = record.getId();

        // When
        serveRecordRepository.deleteById(recordId);

        // Then
        Optional<ServeRecord> deleted = serveRecordRepository.findById(recordId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindByOrderId() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, now);
        ServeRecord record2 = new ServeRecord("V002", "O001", 25, now.plusHours(1)); // Same order
        ServeRecord record3 = new ServeRecord("V003", "O002", 75, now.plusHours(2)); // Different order

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.persist(record3);
        entityManager.flush();

        // When
        List<ServeRecord> orderRecords = serveRecordRepository.findByOrderId("O001");

        // Then
        assertEquals(2, orderRecords.size());
        assertTrue(orderRecords.stream().allMatch(r -> r.getOrderId().equals("O001")));
    }

    @Test
    public void testFindByVehicleId() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, now);
        ServeRecord record2 = new ServeRecord("V001", "O002", 25, now.plusHours(1)); // Same vehicle
        ServeRecord record3 = new ServeRecord("V002", "O003", 75, now.plusHours(2)); // Different vehicle

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.persist(record3);
        entityManager.flush();

        // When
        List<ServeRecord> vehicleRecords = serveRecordRepository.findByVehicleId("V001");

        // Then
        assertEquals(2, vehicleRecords.size());
        assertTrue(vehicleRecords.stream().allMatch(r -> r.getVehicleId().equals("V001")));
    }

    @Test
    public void testFindByServeDateBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime nextWeek = now.plusDays(7);
        
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, yesterday);
        ServeRecord record2 = new ServeRecord("V002", "O002", 75, now);
        ServeRecord record3 = new ServeRecord("V003", "O003", 100, tomorrow);
        ServeRecord record4 = new ServeRecord("V004", "O004", 125, nextWeek);

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.persist(record3);
        entityManager.persist(record4);
        entityManager.flush();

        // When - Find records between yesterday and tomorrow inclusive
        // Use withHour, withMinute, etc. to ensure exact time boundaries for the comparison
        List<ServeRecord> recentRecords = serveRecordRepository.findByServeDateBetween(
            yesterday.withHour(0).withMinute(0).withSecond(0).withNano(0),
            tomorrow.withHour(23).withMinute(59).withSecond(59).withNano(999999999));

        // Then
        assertEquals(3, recentRecords.size()); // Should include yesterday, today, and tomorrow
        assertFalse(recentRecords.stream().anyMatch(r -> r.getVehicleId().equals("V004"))); // Should not include next week
    }
} 
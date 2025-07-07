package com.example.plgsystem.repository;

import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ServeRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ServeRecordRepository serveRecordRepository;
    
    private Vehicle createTestVehicle(String id) {
        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        return entityManager.persist(vehicle);
    }
    
    private Order createTestOrder(String id, LocalDateTime time) {
        Order order = Order.builder()
                .id(id)
                .arrivalTime(time)
                .deadlineTime(time.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(30, 40))
                .build();
        return entityManager.persist(order);
    }

    @Test
    public void testSaveServeRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle = createTestVehicle("V001");
        Order order = createTestOrder("O001", now);
        ServeRecord record = new ServeRecord(vehicle, order, 50, now);

        // When
        ServeRecord savedRecord = serveRecordRepository.save(record);

        // Then
        assertNotNull(savedRecord);
        assertNotNull(savedRecord.getId()); // ID should be generated
        assertEquals("V001", savedRecord.getVehicle().getId());
        assertEquals("O001", savedRecord.getOrder().getId());
        assertEquals(50, savedRecord.getGlpVolumeM3());
        assertEquals(now, savedRecord.getServeDate());
    }

    @Test
    public void testFindById() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle = createTestVehicle("V001");
        Order order = createTestOrder("O001", now);
        ServeRecord record = new ServeRecord(vehicle, order, 50, now);
        entityManager.persist(record);
        entityManager.flush();

        UUID recordId = record.getId();

        // When
        Optional<ServeRecord> found = serveRecordRepository.findById(recordId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(recordId, found.get().getId());
        assertEquals("V001", found.get().getVehicle().getId());
        assertEquals("O001", found.get().getOrder().getId());
        assertEquals(50, found.get().getGlpVolumeM3());
    }

    @Test
    public void testFindAll() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Order order1 = createTestOrder("O001", now);
        Order order2 = createTestOrder("O002", now.plusHours(1));
        
        ServeRecord record1 = new ServeRecord(vehicle1, order1, 50, now);
        ServeRecord record2 = new ServeRecord(vehicle2, order2, 75, now.plusHours(1));

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
        Vehicle vehicle1 = createTestVehicle("V001");
        Order order1 = createTestOrder("O001", now);
        Vehicle vehicle2 = createTestVehicle("V002");
        Order order2 = createTestOrder("O002", now.plusHours(1));
        
        ServeRecord record = new ServeRecord(vehicle1, order1, 50, now);
        entityManager.persist(record);
        entityManager.flush();

        UUID recordId = record.getId();
        
        // Since ServeRecord doesn't have setters, we need to create a new record and update it
        ServeRecord updatedRecord = new ServeRecord(vehicle2, order2, 75, now.plusHours(1));
        // We can't set the ID directly, so we need to delete the old record and save the new one
        serveRecordRepository.deleteById(recordId);
        ServeRecord newRecord = serveRecordRepository.save(updatedRecord);

        // Then
        assertNotNull(newRecord.getId());
        assertEquals(75, newRecord.getGlpVolumeM3());
        assertEquals("V002", newRecord.getVehicle().getId());
        assertEquals("O002", newRecord.getOrder().getId());
    }

    @Test
    public void testDeleteServeRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle = createTestVehicle("V001");
        Order order = createTestOrder("O001", now);
        ServeRecord record = new ServeRecord(vehicle, order, 50, now);
        entityManager.persist(record);
        entityManager.flush();

        UUID recordId = record.getId();

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
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");
        Order order1 = createTestOrder("O001", now);
        Order order2 = createTestOrder("O002", now.plusHours(2));
        
        ServeRecord record1 = new ServeRecord(vehicle1, order1, 50, now);
        ServeRecord record2 = new ServeRecord(vehicle2, order1, 25, now.plusHours(1)); // Same order
        ServeRecord record3 = new ServeRecord(vehicle3, order2, 75, now.plusHours(2)); // Different order

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.persist(record3);
        entityManager.flush();

        // When
        List<ServeRecord> orderRecords = serveRecordRepository.findByOrderId("O001");

        // Then
        assertEquals(2, orderRecords.size());
        assertTrue(orderRecords.stream().allMatch(r -> r.getOrder().getId().equals("O001")));
    }

    @Test
    public void testFindByVehicleId() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Order order1 = createTestOrder("O001", now);
        Order order2 = createTestOrder("O002", now.plusHours(1));
        Order order3 = createTestOrder("O003", now.plusHours(2));
        
        ServeRecord record1 = new ServeRecord(vehicle1, order1, 50, now);
        ServeRecord record2 = new ServeRecord(vehicle1, order2, 25, now.plusHours(1)); // Same vehicle
        ServeRecord record3 = new ServeRecord(vehicle2, order3, 75, now.plusHours(2)); // Different vehicle

        entityManager.persist(record1);
        entityManager.persist(record2);
        entityManager.persist(record3);
        entityManager.flush();

        // When
        List<ServeRecord> vehicleRecords = serveRecordRepository.findByVehicleId("V001");

        // Then
        assertEquals(2, vehicleRecords.size());
        assertTrue(vehicleRecords.stream().allMatch(r -> r.getVehicle().getId().equals("V001")));
    }

    @Test
    public void testFindByServeDateBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime nextWeek = now.plusDays(7);
        
        Vehicle vehicle1 = createTestVehicle("V001");
        Vehicle vehicle2 = createTestVehicle("V002");
        Vehicle vehicle3 = createTestVehicle("V003");
        Vehicle vehicle4 = createTestVehicle("V004");
        
        Order order1 = createTestOrder("O001", yesterday);
        Order order2 = createTestOrder("O002", now);
        Order order3 = createTestOrder("O003", tomorrow);
        Order order4 = createTestOrder("O004", nextWeek);
        
        ServeRecord record1 = new ServeRecord(vehicle1, order1, 50, yesterday);
        ServeRecord record2 = new ServeRecord(vehicle2, order2, 75, now);
        ServeRecord record3 = new ServeRecord(vehicle3, order3, 100, tomorrow);
        ServeRecord record4 = new ServeRecord(vehicle4, order4, 125, nextWeek);

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
        assertFalse(recentRecords.stream().anyMatch(r -> r.getVehicle().getId().equals("V004"))); // Should not include next week
    }
} 
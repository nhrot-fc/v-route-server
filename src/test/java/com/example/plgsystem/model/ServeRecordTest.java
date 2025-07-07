package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ServeRecordTest {

    private ServeRecord serveRecord;
    private Vehicle vehicle;
    private Order order;
    private final int volumeM3 = 100;
    private final LocalDateTime serveDate = LocalDateTime.now();

    @BeforeEach
    public void setUp() {
        // Crear Vehicle
        Position vehiclePosition = new Position(10, 20);
        vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(vehiclePosition)
                .build();
        
        // Crear Order
        Position orderPosition = new Position(15, 25);
        LocalDateTime arrivalTime = LocalDateTime.now().minusHours(2);
        LocalDateTime deadlineTime = LocalDateTime.now().plusHours(24);
        order = Order.builder()
                .id("O-001")
                .arrivalTime(arrivalTime)
                .deadlineTime(deadlineTime)
                .glpRequestM3(500)
                .position(orderPosition)
                .build();
        
        // Crear ServeRecord
        serveRecord = new ServeRecord(vehicle, order, volumeM3, serveDate);
    }

    @Test
    public void testServeRecordCreation() {
        assertNotNull(serveRecord);
        assertNotNull(serveRecord.getId()); // ID should be automatically generated UUID
        assertTrue(serveRecord.getId() instanceof UUID);
        assertEquals(vehicle, serveRecord.getVehicle());
        assertEquals(order, serveRecord.getOrder());
        assertEquals(volumeM3, serveRecord.getGlpVolumeM3());
        assertEquals(serveDate, serveRecord.getServeDate());
    }

    @Test
    public void testServeRecordProperties() {
        // Verificar propiedades básicas
        assertEquals("V-001", serveRecord.getVehicle().getId());
        assertEquals("O-001", serveRecord.getOrder().getId());
        assertEquals(volumeM3, serveRecord.getGlpVolumeM3());
        
        // Verificar objetos relacionados
        assertSame(vehicle, serveRecord.getVehicle());
        assertSame(order, serveRecord.getOrder());
    }

    @Test
    public void testCreateMultipleServeRecordsForOrder() {
        // Setup
        Position orderPosition = new Position(15, 25);
        LocalDateTime arrivalTime = LocalDateTime.now().minusHours(2);
        LocalDateTime deadlineTime = LocalDateTime.now().plusHours(24);
        Order testOrder = Order.builder()
                .id("O-002")
                .arrivalTime(arrivalTime)
                .deadlineTime(deadlineTime)
                .glpRequestM3(1000)
                .position(orderPosition)
                .build();
        
        // Verify initial state
        assertEquals(1000, testOrder.getRemainingGlpM3());
        assertEquals(0, testOrder.getServeRecords().size());
        
        // First service - partial delivery
        LocalDateTime firstServeTime = LocalDateTime.now();
        ServeRecord firstRecord = testOrder.recordDelivery(400, vehicle, firstServeTime);
        
        // Verify first record
        assertNotNull(firstRecord);
        assertEquals(400, firstRecord.getGlpVolumeM3());
        assertEquals(vehicle, firstRecord.getVehicle());
        assertEquals(testOrder, firstRecord.getOrder());
        assertEquals(firstServeTime, firstRecord.getServeDate());
        
        // Verify order state after first delivery
        assertEquals(600, testOrder.getRemainingGlpM3());
        assertEquals(1, testOrder.getServeRecords().size());
        assertFalse(testOrder.isDelivered());
        
        // Second service - remaining delivery
        LocalDateTime secondServeTime = LocalDateTime.now().plusHours(1);
        ServeRecord secondRecord = testOrder.recordDelivery(600, vehicle, secondServeTime);
        
        // Verify second record
        assertNotNull(secondRecord);
        assertEquals(600, secondRecord.getGlpVolumeM3());
        
        // Verify order state after second delivery
        assertEquals(0, testOrder.getRemainingGlpM3());
        assertEquals(2, testOrder.getServeRecords().size());
        assertTrue(testOrder.isDelivered());
    }
} 
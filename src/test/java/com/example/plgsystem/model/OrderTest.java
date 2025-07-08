package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    private Order order;
    private Vehicle vehicle;
    private final String orderId = "O-001";
    private final Position position = new Position(10, 20);
    private final int glpRequestM3 = 100;
    private final LocalDateTime arrivalTime = LocalDateTime.now();
    private final LocalDateTime deadlineTime = arrivalTime.plusHours(24);

    @BeforeEach
    public void setUp() {
        // Crear Order
        order = Order.builder()
                .id(orderId)
                .arrivalTime(arrivalTime)
                .deadlineTime(deadlineTime)
                .glpRequestM3(glpRequestM3)
                .position(position)
                .build();
                
        // Crear Vehicle para pruebas
        Position vehiclePos = new Position(5, 5);
        vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(vehiclePos)
                .build();
    }

    @Test
    public void testOrderCreation() {
        assertNotNull(order);
        assertEquals(orderId, order.getId());
        assertEquals(position, order.getPosition());
        assertEquals(glpRequestM3, order.getGlpRequestM3());
        assertEquals(glpRequestM3, order.getRemainingGlpM3());
        assertEquals(arrivalTime, order.getArrivalTime());
        assertEquals(deadlineTime, order.getDeadlineTime());
        assertFalse(order.isDelivered());
    }

    @Test
    public void testRecordDelivery() {
        // Entrega parcial (50% del total)
        int deliveredVolume = glpRequestM3 / 2;
        LocalDateTime serveDate = LocalDateTime.now();
        
        ServeRecord record = order.recordDelivery(deliveredVolume, vehicle, serveDate);
        
        assertNotNull(record);
        assertEquals(vehicle, record.getVehicle());
        assertEquals(order, record.getOrder());
        assertEquals(deliveredVolume, record.getGlpVolumeM3());
        assertEquals(serveDate, record.getServeDate());
        
        // Verificar que el estado de la orden cambió
        assertEquals(glpRequestM3 - deliveredVolume, order.getRemainingGlpM3());
        assertFalse(order.isDelivered());
        
        // Completar la entrega
        order.recordDelivery(glpRequestM3 / 2, vehicle, serveDate.plusHours(1));
        assertEquals(0, order.getRemainingGlpM3());
        assertTrue(order.isDelivered());
    }
    
    @Test
    public void testRecordOverDelivery() {
        // Entregar más de lo solicitado (debería establecer el remanente en 0)
        ServeRecord record = order.recordDelivery(glpRequestM3 + 50, vehicle, LocalDateTime.now());
        
        assertEquals(glpRequestM3 + 50, record.getGlpVolumeM3());
        assertEquals(0, order.getRemainingGlpM3());
        assertTrue(order.isDelivered());
        assertEquals(1, order.getServeRecords().size());
    }

    @Test
    public void testIsOverdue() {
        // Todavía no está vencida
        assertFalse(order.isOverdue(deadlineTime.minusHours(1)));
        
        // Exactamente en el tiempo límite no está vencida
        assertFalse(order.isOverdue(deadlineTime));
        
        // Después del tiempo límite está vencida
        assertTrue(order.isOverdue(deadlineTime.plusMinutes(1)));
    }

    @Test
    public void testMultipleDeliveries() {
        // Setup
        LocalDateTime firstDelivery = LocalDateTime.now();
        LocalDateTime secondDelivery = firstDelivery.plusHours(2);
        
        // Primera entrega (30%)
        ServeRecord record1 = order.recordDelivery(30, vehicle, firstDelivery);
        assertEquals(30, record1.getGlpVolumeM3());
        assertEquals(glpRequestM3 - 30, order.getRemainingGlpM3());
        
        // Segunda entrega (40%)
        ServeRecord record2 = order.recordDelivery(40, vehicle, secondDelivery);
        assertEquals(40, record2.getGlpVolumeM3());
        assertEquals(glpRequestM3 - 70, order.getRemainingGlpM3());
        
        // Verificar que se agregaron los dos registros a la orden
        assertEquals(2, order.getServeRecords().size());
        assertTrue(order.getServeRecords().contains(record1));
        assertTrue(order.getServeRecords().contains(record2));
    }
    
    @Test
    public void testOrderCompletion() {
        // Verificar inicialmente no entregada
        assertFalse(order.isDelivered());
        
        // Completar la orden
        order.recordDelivery(glpRequestM3, vehicle, LocalDateTime.now());
        
        // Verificar ahora está entregada
        assertTrue(order.isDelivered());
        assertEquals(0, order.getRemainingGlpM3());
        
        // Intentar entregar más no debería cambiar el estado
        order.recordDelivery(10, vehicle, LocalDateTime.now().plusHours(1));
        assertTrue(order.isDelivered());
        assertEquals(0, order.getRemainingGlpM3());
    }
} 
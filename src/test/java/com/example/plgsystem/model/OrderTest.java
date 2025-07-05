package com.example.plgsystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    private Order order;
    private final String orderId = "O001";
    private final Position position = new Position(10, 20);
    private final int glpRequestM3 = 100;
    private final LocalDateTime arriveTime = LocalDateTime.now();
    private final LocalDateTime dueTime = arriveTime.plusHours(2);

    @BeforeEach
    public void setUp() {
        order = Order.builder()
                .id(orderId)
                .arriveTime(arriveTime)
                .dueTime(dueTime)
                .glpRequestM3(glpRequestM3)
                .position(position)
                .build();
    }

    @Test
    public void testOrderCreation() {
        assertNotNull(order);
        assertEquals(orderId, order.getId());
        assertEquals(position, order.getPosition());
        assertEquals(glpRequestM3, order.getGlpRequestM3());
        assertEquals(glpRequestM3, order.getRemainingGlpM3());
        assertEquals(arriveTime, order.getArriveTime());
        // Due time is set directly to the provided value
        assertEquals(dueTime, order.getDueTime());
        assertFalse(order.isDelivered());
    }

    @Test
    public void testRecordDelivery() {
        // Partial delivery (50% of total)
        int deliveredVolume = glpRequestM3 / 2;
        String vehicleId = "V001";
        LocalDateTime serveDate = LocalDateTime.now();
        
        ServeRecord record = order.recordDelivery(deliveredVolume, vehicleId, serveDate);
        
        assertNotNull(record);
        assertEquals(vehicleId, record.getVehicleId());
        assertEquals(orderId, record.getOrderId());
        assertEquals(deliveredVolume, record.getVolumeM3());
        assertEquals(serveDate, record.getServeDate());
        
        // Verify order state changed
        assertEquals(glpRequestM3 - deliveredVolume, order.getRemainingGlpM3());
        assertFalse(order.isDelivered());
        
        // Complete the delivery
        record = order.recordDelivery(glpRequestM3 / 2, vehicleId, serveDate);
        assertEquals(0, order.getRemainingGlpM3());
        assertTrue(order.isDelivered());
    }
    
    @Test
    public void testRecordOverDelivery() {
        // Deliver more than requested (should cap at 0 remaining)
        ServeRecord record = order.recordDelivery(glpRequestM3 + 50, "V001", LocalDateTime.now());
        
        assertEquals(glpRequestM3 + 50, record.getVolumeM3());
        assertEquals(0, order.getRemainingGlpM3());
        assertTrue(order.isDelivered());
    }

    @Test
    public void testIsOverdue() {
        // Not overdue yet
        assertFalse(order.isOverdue(dueTime.minusHours(1)));
        
        // Exactly at due time is not overdue
        assertFalse(order.isOverdue(order.getDueTime()));
        
        // After due time is overdue
        assertTrue(order.isOverdue(order.getDueTime().plusMinutes(1)));
    }

    @Test
    public void testTimeUntilDue() {
        LocalDateTime referenceTime = order.getDueTime().minusHours(2);
        int minutesUntilDue = order.timeUntilDue(referenceTime);
        
        assertEquals(120, minutesUntilDue);
        
        // If already delivered, should return 0
        order.recordDelivery(glpRequestM3, "V001", LocalDateTime.now());
        assertEquals(0, order.timeUntilDue(referenceTime));
        
        // If overdue, should return -1
        order = Order.builder()
                .id(orderId)
                .arriveTime(arriveTime.minusHours(6))
                .dueTime(arriveTime.minusHours(1))
                .glpRequestM3(glpRequestM3)
                .position(position)
                .build();
                
        assertEquals(-1, order.timeUntilDue(LocalDateTime.now()));
    }

    @Test
    public void testCalculatePriority() {
        // Normal priority
        LocalDateTime reference = order.getDueTime().minusHours(6);
        double priority = order.calculatePriority(reference);
        assertTrue(priority > 0 && priority < 100);
        
        // High priority (almost due)
        reference = order.getDueTime().minusMinutes(10);
        priority = order.calculatePriority(reference);
        assertTrue(priority > 10);
        
        // Overdue order (highest priority)
        reference = order.getDueTime().plusHours(2);
        priority = order.calculatePriority(reference);
        assertTrue(priority > 1000);
        
        // Delivered order (no priority)
        order.recordDelivery(glpRequestM3, "V001", LocalDateTime.now());
        priority = order.calculatePriority(reference);
        assertEquals(0.0, priority);
    }

    @Test
    public void testClone() {
        Order clonedOrder = order.clone();
        
        assertNotSame(order, clonedOrder);
        assertEquals(order.getId(), clonedOrder.getId());
        assertEquals(order.getGlpRequestM3(), clonedOrder.getGlpRequestM3());
        assertEquals(order.getRemainingGlpM3(), clonedOrder.getRemainingGlpM3());
        assertEquals(order.getDueTime(), clonedOrder.getDueTime());
        
        // Position should also be cloned
        assertNotSame(order.getPosition(), clonedOrder.getPosition());
        assertEquals(order.getPosition().getX(), clonedOrder.getPosition().getX());
        assertEquals(order.getPosition().getY(), clonedOrder.getPosition().getY());
    }

    @Test
    public void testToString() {
        String orderString = order.toString();
        
        assertTrue(orderString.contains(orderId));
        assertTrue(orderString.contains(String.valueOf(glpRequestM3)));
        
        // After delivery, should show a different status
        order.recordDelivery(glpRequestM3, "V001", LocalDateTime.now());
        String deliveredOrderString = order.toString();
        assertTrue(deliveredOrderString.contains("✅"));
    }
    
    @Test
    public void testSetRemainingGlpM3() {
        order.setRemainingGlpM3(50);
        assertEquals(50, order.getRemainingGlpM3());
        assertFalse(order.isDelivered());
        
        order.setRemainingGlpM3(0);
        assertEquals(0, order.getRemainingGlpM3());
        assertTrue(order.isDelivered());
    }
} 
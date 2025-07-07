package com.example.plgsystem.assignation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class DeliveryPartTest {

    @Test
    void testDeliveryPartCreation() {
        // Arrange
        String orderId = "ORD-123";
        int glpDeliverM3 = 10;
        LocalDateTime deadlineTime = LocalDateTime.now().plusHours(2);

        // Act
        DeliveryPart deliveryPart = new DeliveryPart(orderId, glpDeliverM3, deadlineTime);

        // Assert
        assertEquals(orderId, deliveryPart.getOrderId());
        assertEquals(glpDeliverM3, deliveryPart.getGlpDeliverM3());
        assertEquals(deadlineTime, deliveryPart.getDeadlineTime());
    }
} 
package com.example.plgsystem.assignation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DeliveryConsolidatorTest {

    @Test
    void mergeLikeRLE_shouldMergePerfectlyWithExactCapacity() {
        // Arrange
        List<DeliveryPart> parts = List.of(
                createDeliveryPart("O1", 5),
                createDeliveryPart("O1", 5),
                createDeliveryPart("O1", 5),
                createDeliveryPart("O1", 5));

        int vehicleCapacity = 10;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(parts, vehicleCapacity);

        // Assert
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(10, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
    }

    @Test
    void mergeLikeRLE_shouldMergeWithRemainder() {
        // Arrange
        List<DeliveryPart> parts = List.of(
                createDeliveryPart("O1", 7),
                createDeliveryPart("O1", 7),
                createDeliveryPart("O1", 7),
                createDeliveryPart("O1", 7),
                createDeliveryPart("O1", 7));

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(parts, vehicleCapacity);

        // Assert
        assertEquals(15, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(15, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(5, result.get(2).getGlpDeliverM3());
        assertEquals("O1", result.get(2).getOrderId());
        assertEquals(3, result.size());
    }

    @Test
    void mergeLikeRLE_shouldHandleMultipleOrders() {
        // Arrange
        List<DeliveryPart> parts = List.of(
                createDeliveryPart("O1", 10),
                createDeliveryPart("O1", 10),
                createDeliveryPart("O2", 5),
                createDeliveryPart("O2", 10),
                createDeliveryPart("O1", 10));

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(parts, vehicleCapacity);

        // Assert
        assertEquals(15, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(5, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(15, result.get(2).getGlpDeliverM3());
        assertEquals("O2", result.get(2).getOrderId());
        assertEquals(10, result.get(3).getGlpDeliverM3());
        assertEquals("O1", result.get(3).getOrderId());
        assertEquals(4, result.size());
    }

    @Test
    void splitConsecutiveAndAtomic_shouldSplitConsecutiveParts() {
        // Arrange
        List<DeliveryPart> parts = List.of(
                createDeliveryPart("O1", 7),
                createDeliveryPart("O1", 3),
                createDeliveryPart("O2", 8));

        int atomicSize = 5;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.splitConsecutiveAndAtomic(parts, atomicSize);

        // Assert
        assertEquals(4, result.size());
        assertEquals(5, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(5, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(5, result.get(2).getGlpDeliverM3());
        assertEquals("O2", result.get(2).getOrderId());
        assertEquals(3, result.get(3).getGlpDeliverM3());
        assertEquals("O2", result.get(3).getOrderId());
    }

    @Test
    void splitConsecutiveAndAtomic_shouldHandleNonConsecutiveParts() {
        // Arrange
        List<DeliveryPart> parts = List.of(
                createDeliveryPart("O1", 12),
                createDeliveryPart("O2", 4),
                createDeliveryPart("O1", 8));

        int atomicSize = 5;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.splitConsecutiveAndAtomic(parts, atomicSize);

        // Assert
        assertEquals(6, result.size());
        assertEquals(5, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(5, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(2, result.get(2).getGlpDeliverM3());
        assertEquals("O1", result.get(2).getOrderId());
        assertEquals(4, result.get(3).getGlpDeliverM3());
        assertEquals("O2", result.get(3).getOrderId());
        assertEquals(5, result.get(4).getGlpDeliverM3());
        assertEquals("O1", result.get(4).getOrderId());
        assertEquals(3, result.get(5).getGlpDeliverM3());
        assertEquals("O1", result.get(5).getOrderId());
    }

    @Test
    void completeFlow_shouldSplitAndThenMergeEfficiently() {
        // Arrange
        List<DeliveryPart> initialParts = List.of(
                createDeliveryPart("O1", 12),
                createDeliveryPart("O1", 3),
                createDeliveryPart("O2", 17));

        int atomicSize = 5;
        int vehicleCapacity = 20;

        // Act - First split
        List<DeliveryPart> atomicParts = DeliveryConsolidator.splitConsecutiveAndAtomic(initialParts, atomicSize);

        // Verify the intermediate result
        assertEquals(7, atomicParts.size());
        assertEquals(5, atomicParts.get(0).getGlpDeliverM3());
        assertEquals("O1", atomicParts.get(0).getOrderId());
        assertEquals(5, atomicParts.get(1).getGlpDeliverM3());
        assertEquals("O1", atomicParts.get(1).getOrderId());
        assertEquals(5, atomicParts.get(2).getGlpDeliverM3());
        assertEquals("O1", atomicParts.get(2).getOrderId());
        assertEquals(5, atomicParts.get(3).getGlpDeliverM3());
        assertEquals("O2", atomicParts.get(3).getOrderId());
        assertEquals(5, atomicParts.get(4).getGlpDeliverM3());
        assertEquals("O2", atomicParts.get(4).getOrderId());
        assertEquals(5, atomicParts.get(5).getGlpDeliverM3());
        assertEquals("O2", atomicParts.get(5).getOrderId());
        assertEquals(2, atomicParts.get(6).getGlpDeliverM3());
        assertEquals("O2", atomicParts.get(6).getOrderId());

        // Act - Then merge
        List<DeliveryPart> finalResult = DeliveryConsolidator.mergeLikeRLE(atomicParts, vehicleCapacity);

        // Assert
        assertEquals(2, finalResult.size());
        assertEquals(15, finalResult.get(0).getGlpDeliverM3());
        assertEquals("O1", finalResult.get(0).getOrderId());
        assertEquals(17, finalResult.get(1).getGlpDeliverM3());
        assertEquals("O2", finalResult.get(1).getOrderId());
    }

    @Test
    void mergeLikeRLE_shouldHandleEmptyList() {
        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(new ArrayList<>(), 10);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void splitConsecutiveAndAtomic_shouldHandleEmptyList() {
        // Act
        List<DeliveryPart> result = DeliveryConsolidator.splitConsecutiveAndAtomic(new ArrayList<>(), 5);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void mergeLikeRLE_shouldHandleNegativeCapacity() {
        // Arrange
        List<DeliveryPart> parts = List.of(createDeliveryPart("O1", 5));

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(parts, -1);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void splitConsecutiveAndAtomic_shouldHandleNegativeAtomicSize() {
        // Arrange
        List<DeliveryPart> parts = List.of(createDeliveryPart("O1", 5));

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.splitConsecutiveAndAtomic(parts, -1);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void mergeLikeRLE_shouldHandleSpecificOptimizerTestCase() {
        // Arrange - This replicates the specific case from DeliveryOptimizerTest
        List<DeliveryPart> atomicParts = List.of(
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O1", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1),
                createDeliveryPart("O2", 1));

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(atomicParts, vehicleCapacity);

        // Assert
        assertEquals(3, result.size());
        assertEquals(15, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(5, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(7, result.get(2).getGlpDeliverM3());
        assertEquals("O2", result.get(2).getOrderId());
    }

    @Test
    void mergeLikeRLE_shouldProperlyMergeRemainder() {
        // Arrange
        List<DeliveryPart> atomicParts = List.of(
                createDeliveryPart("O1", 12),
                createDeliveryPart("O1", 8),
                createDeliveryPart("O1", 7));

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(atomicParts, vehicleCapacity);

        // Assert
        assertEquals(2, result.size());
        assertEquals(15, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(12, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
    }

    @Test
    void mergeLikeRLE_shouldHandleConsecutivePartialFills() {
        // Arrange
        List<DeliveryPart> atomicParts = List.of(
                createDeliveryPart("O1", 10),
                createDeliveryPart("O1", 10),
                createDeliveryPart("O1", 5),
                createDeliveryPart("O2", 7));

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(atomicParts, vehicleCapacity);

        // Assert
        assertEquals(3, result.size());
        assertEquals(15, result.get(0).getGlpDeliverM3());
        assertEquals("O1", result.get(0).getOrderId());
        assertEquals(10, result.get(1).getGlpDeliverM3());
        assertEquals("O1", result.get(1).getOrderId());
        assertEquals(7, result.get(2).getGlpDeliverM3());
        assertEquals("O2", result.get(2).getOrderId());
    }

    @Test
    void mergeLikeRLE_shouldCorrectlyMergeBasedOnOptimizerTest() {
        // Arrange - This replicates the specific case in DeliveryOptimizerTest
        List<DeliveryPart> atomicParts = new ArrayList<>();
        // Add 20 units of O1
        for (int i = 0; i < 20; i++) {
            atomicParts.add(createDeliveryPart("O1", 1));
        }
        // Add 7 units of O2
        for (int i = 0; i < 7; i++) {
            atomicParts.add(createDeliveryPart("O2", 1));
        }

        int vehicleCapacity = 15;

        // Act
        List<DeliveryPart> result = DeliveryConsolidator.mergeLikeRLE(atomicParts, vehicleCapacity);

        // Assert - Verify that we get [15(O1), 5(O1), 7(O2)]
        assertEquals(3, result.size(), "Should have 3 delivery parts");
        assertEquals(15, result.get(0).getGlpDeliverM3(), "First part should be 15 units");
        assertEquals("O1", result.get(0).getOrderId(), "First part should be order O1");
        assertEquals(5, result.get(1).getGlpDeliverM3(), "Second part should be 5 units");
        assertEquals("O1", result.get(1).getOrderId(), "Second part should be order O1");
        assertEquals(7, result.get(2).getGlpDeliverM3(), "Third part should be 7 units");
        assertEquals("O2", result.get(2).getOrderId(), "Third part should be order O2");
    }

    private DeliveryPart createDeliveryPart(String orderId, int glpAmount) {
        return new DeliveryPart(orderId, glpAmount, LocalDateTime.now().plusHours(2));
    }
}

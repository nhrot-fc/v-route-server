package com.example.plgsystem.repository;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
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
public class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testSaveOrder() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertNotNull(savedOrder);
        assertEquals("O001", savedOrder.getId());
        assertEquals(100, savedOrder.getGlpRequestM3());
        assertEquals(100, savedOrder.getRemainingGlpM3());
    }

    @Test
    public void testFindById() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();
        entityManager.persist(order);
        entityManager.flush();

        // When
        Optional<Order> found = orderRepository.findById("O001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("O001", found.get().getId());
        assertEquals(100, found.get().getGlpRequestM3());
    }

    @Test
    public void testFindAll() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Order order1 = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        Order order2 = Order.builder()
                .id("O002")
                .arriveTime(now.plusHours(1))
                .dueTime(now.plusHours(5))
                .glpRequestM3(150)
                .position(new Position(30, 40))
                .build();

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // When
        List<Order> orders = orderRepository.findAll();

        // Then
        assertEquals(2, orders.size());
        assertTrue(orders.stream().anyMatch(o -> o.getId().equals("O001")));
        assertTrue(orders.stream().anyMatch(o -> o.getId().equals("O002")));
    }

    @Test
    public void testUpdateOrder() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();
        entityManager.persist(order);
        entityManager.flush();

        // When
        Order savedOrder = orderRepository.findById("O001").get();
        savedOrder.setRemainingGlpM3(50); // Partially delivered
        orderRepository.save(savedOrder);

        // Then
        Order updatedOrder = orderRepository.findById("O001").get();
        assertEquals(50, updatedOrder.getRemainingGlpM3());
        assertEquals(100, updatedOrder.getGlpRequestM3()); // Original request remains unchanged
    }

    @Test
    public void testDeleteOrder() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();
        entityManager.persist(order);
        entityManager.flush();

        // When
        orderRepository.deleteById("O001");

        // Then
        Optional<Order> deleted = orderRepository.findById("O001");
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testFindPendingDeliveries() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Order with pending delivery
        Order pendingOrder = Order.builder()
                .id("O001")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        // Order with completed delivery
        Order completedOrder = Order.builder()
                .id("O002")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(150)
                .position(new Position(30, 40))
                .build();
        completedOrder.setRemainingGlpM3(0); // Mark as delivered

        entityManager.persist(pendingOrder);
        entityManager.persist(completedOrder);
        entityManager.flush();

        // When
        List<Order> pendingOrders = orderRepository.findPendingDeliveries();

        // Then
        assertEquals(1, pendingOrders.size());
        assertEquals("O001", pendingOrders.get(0).getId());
    }

    @Test
    public void testFindByDueTimeBefore() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Order due soon
        Order soonDueOrder = Order.builder()
                .id("O001")
                .arriveTime(now.minusHours(2))
                .dueTime(now.plusHours(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        // Order due later
        Order laterDueOrder = Order.builder()
                .id("O002")
                .arriveTime(now)
                .dueTime(now.plusHours(4))
                .glpRequestM3(150)
                .position(new Position(30, 40))
                .build();

        entityManager.persist(soonDueOrder);
        entityManager.persist(laterDueOrder);
        entityManager.flush();

        // When
        List<Order> overdueOrders = orderRepository.findByDueTimeBefore(now.plusHours(2));

        // Then
        assertEquals(1, overdueOrders.size());
        assertEquals("O001", overdueOrders.get(0).getId());
    }

    @Test
    public void testFindByArriveTimeLessThanEqual() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Order already arrived
        Order arrivedOrder = Order.builder()
                .id("O001")
                .arriveTime(now.minusHours(1))
                .dueTime(now.plusHours(3))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        // Order not yet arrived
        Order futureOrder = Order.builder()
                .id("O002")
                .arriveTime(now.plusHours(2))
                .dueTime(now.plusHours(6))
                .glpRequestM3(150)
                .position(new Position(30, 40))
                .build();

        entityManager.persist(arrivedOrder);
        entityManager.persist(futureOrder);
        entityManager.flush();

        // When
        List<Order> availableOrders = orderRepository.findByArriveTimeLessThanEqual(now);

        // Then
        assertEquals(1, availableOrders.size());
        assertEquals("O001", availableOrders.get(0).getId());
    }
} 
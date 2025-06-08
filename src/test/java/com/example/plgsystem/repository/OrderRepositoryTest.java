package com.example.plgsystem.repository;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testOrderPersistence() {
        // Create and save an order
        Position orderPosition = new Position(20, 30);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deliveryDue = now.plusHours(8);
        
        Order order = new Order("ORD-001", now, deliveryDue, 500.0, orderPosition);
        
        Order savedOrder = orderRepository.save(order);
        
        // Assert the order was saved correctly
        assertThat(savedOrder.getId()).isEqualTo("ORD-001");
        assertThat(savedOrder.getGlpRequest()).isEqualTo(500.0);
        assertThat(savedOrder.getRemainingGLP()).isEqualTo(500.0);
        assertThat(savedOrder.getPosition().getX()).isEqualTo(20);
        assertThat(savedOrder.getPosition().getY()).isEqualTo(30);
        assertThat(savedOrder.getArriveDate()).isEqualToIgnoringNanos(now);
        assertThat(savedOrder.getDueDate()).isEqualToIgnoringNanos(deliveryDue);
    }

    @Test
    public void testFindPendingOrders() {
        // Create pending order (not fully fulfilled)
        Position pos1 = new Position(10, 20);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deliveryDue1 = now.plusHours(4);
        
        Order pendingOrder = new Order("ORD-PEND-001", now, deliveryDue1, 1000.0, pos1);
        pendingOrder.setRemainingGLP(500.0); // Partially fulfilled
        orderRepository.save(pendingOrder);
        
        // Create completed order
        Position pos2 = new Position(30, 40);
        LocalDateTime deliveryDue2 = now.plusHours(6);
        
        Order completedOrder = new Order("ORD-COMP-001", now, deliveryDue2, 800.0, pos2);
        completedOrder.setRemainingGLP(0.0); // Fully fulfilled
        orderRepository.save(completedOrder);
        
        // Test finding pending orders
        List<Order> pendingOrders = orderRepository.findPendingOrders();
        
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getId()).isEqualTo("ORD-PEND-001");
    }

    @Test
    public void testFindOrdersByDueDate() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create orders with different due dates
        // Order due in 2 hours
        Position pos1 = new Position(10, 20);
        LocalDateTime due1 = now.plusHours(2);
        Order order1 = new Order("ORD-DUE-1", now, due1, 300.0, pos1);
        orderRepository.save(order1);
        
        // Order due in 6 hours
        Position pos2 = new Position(30, 40);
        LocalDateTime due2 = now.plusHours(6);
        Order order2 = new Order("ORD-DUE-2", now, due2, 400.0, pos2);
        orderRepository.save(order2);
        
        // Order due in 12 hours
        Position pos3 = new Position(50, 60);
        LocalDateTime due3 = now.plusHours(12);
        Order order3 = new Order("ORD-DUE-3", now, due3, 500.0, pos3);
        orderRepository.save(order3);
        
        // Test finding orders due in the next 4 hours
        List<Order> urgentOrders = orderRepository.findOrdersByDueDate(now, now.plusHours(4));
        
        assertThat(urgentOrders).hasSize(1);
        assertThat(urgentOrders.get(0).getId()).isEqualTo("ORD-DUE-1");
        
        // Test finding orders due in the next 8 hours
        List<Order> soonDueOrders = orderRepository.findOrdersByDueDate(now, now.plusHours(8));
        
        assertThat(soonDueOrders).hasSize(2);
        assertThat(soonDueOrders).extracting("id").contains("ORD-DUE-1", "ORD-DUE-2");
    }

    @Test
    public void testFindOrdersByRadius() {
        // Create orders at different locations
        Position center = new Position(50, 50);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusHours(4);
        
        // Order near the center point
        Position pos1 = new Position(55, 55);
        Order nearOrder = new Order("ORD-NEAR-1", now, due, 200.0, pos1);
        orderRepository.save(nearOrder);
        
        // Order far from the center point
        Position pos2 = new Position(80, 80);
        Order farOrder = new Order("ORD-FAR-1", now, due, 300.0, pos2);
        orderRepository.save(farOrder);
        
        // Test finding orders within 10 distance units from center
        List<Order> nearbyOrders = orderRepository.findOrdersByRadius(center.getX(), center.getY(), 10.0);
        
        assertThat(nearbyOrders).hasSize(1);
        assertThat(nearbyOrders.get(0).getId()).isEqualTo("ORD-NEAR-1");
        
        // Test finding orders within 50 distance units from center (should include both)
        List<Order> allOrders = orderRepository.findOrdersByRadius(center.getX(), center.getY(), 50.0);
        
        assertThat(allOrders).hasSize(2);
    }

    @Test
    public void testFindOrdersByDateRange() {
        // Create orders from different times
        Position pos = new Position(30, 30);
        
        // Yesterday's order
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime yesterdayDue = yesterday.plusHours(4);
        Order yesterdayOrder = new Order("ORD-YESTERDAY", yesterday, yesterdayDue, 400.0, pos);
        orderRepository.save(yesterdayOrder);
        
        // Today's order
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime todayDue = today.plusHours(4);
        Order todayOrder = new Order("ORD-TODAY", today, todayDue, 500.0, pos);
        orderRepository.save(todayOrder);
        
        // Tomorrow's order
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime tomorrowDue = tomorrow.plusHours(4);
        Order tomorrowOrder = new Order("ORD-TOMORROW", tomorrow, tomorrowDue, 600.0, pos);
        orderRepository.save(tomorrowOrder);
        
        // Test finding orders from today
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);
        
        List<Order> todaysOrders = orderRepository.findOrdersByDateRange(startOfToday, endOfToday);
        
        assertThat(todaysOrders).hasSize(1);
        assertThat(todaysOrders.get(0).getId()).isEqualTo("ORD-TODAY");
        
        // Test finding orders for a 3-day period (yesterday to tomorrow)
        List<Order> threeDayOrders = orderRepository.findOrdersByDateRange(yesterday.minusHours(1), tomorrow.plusHours(1));
        
        assertThat(threeDayOrders).hasSize(3);
    }
}

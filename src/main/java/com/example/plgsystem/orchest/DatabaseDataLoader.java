package com.example.plgsystem.orchest;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of DataLoader that loads data from the database.
 */
public class DatabaseDataLoader implements DataLoader {

    private final OrderRepository orderRepository;
    private final BlockageRepository blockageRepository;

    /**
     * Constructor for DatabaseDataLoader
     * 
     * @param orderRepository    Repository for orders
     * @param blockageRepository Repository for blockages
     */
    public DatabaseDataLoader(
            OrderRepository orderRepository,
            BlockageRepository blockageRepository) {
        this.orderRepository = orderRepository;
        this.blockageRepository = blockageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> loadOrdersForDate(LocalDate date) {
        // Set the time range for the day
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // Find orders that arrive on the specified date
        List<Order> orders = orderRepository.findPendingByArrivalTimeBetween(startOfDay, endOfDay);
        List<Event> events = new ArrayList<>();

        if (!orders.isEmpty()) {
            // Create events for each order
            for (Order order : orders) {
                Event event = new Event(EventType.ORDER, order.getArrivalTime(), order.getId(), order);
                events.add(event);
            }
        }

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> loadBlockagesForDate(LocalDate date) {
        // Set the time range for the day
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // Find active blockages on the specified date
        List<Blockage> blockages = blockageRepository.findActiveBlockagesForPeriod(startOfDay, endOfDay);
        List<Event> events = new ArrayList<>();

        if (!blockages.isEmpty()) {
            // Create events for each blockage
            for (Blockage blockage : blockages) {
                // Start event for the blockage
                Event startEvent = new Event(
                        EventType.BLOCKAGE,
                        blockage.getStartTime(),
                        blockage.getId().toString(),
                        blockage);
                events.add(startEvent);
            }
        }

        return events;
    }
}
package com.example.plgsystem.orchest;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SimpleDataLoader implements DataLoader {
    private final PriorityQueue<Event> events;

    public SimpleDataLoader() {
        this.events = new PriorityQueue<>(Comparator.comparing(Event::getTime));
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    @Override
    public List<Event> loadOrdersForDate(LocalDate date) {
        return events.stream().filter(event -> event.getType() == EventType.ORDER_ARRIVAL)
                .filter(event -> event.getTime().toLocalDate().equals(date)).toList();
    }

    @Override
    public List<Event> loadBlockagesForDate(LocalDate date) {
        return events.stream().filter(event -> event.getType() == EventType.BLOCKAGE_START)
                .filter(event -> event.getTime().toLocalDate().equals(date)).toList();
    }
}

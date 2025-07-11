package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.List;
import com.example.plgsystem.model.Constants;

public record Route(String vehicleId, List<RouteStop> stops, LocalDateTime startTime) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🛣️ Route { 🚚 vehicleId: %s, 🕒 startTime: %s }\n", 
                vehicleId, startTime.format(Constants.DATE_TIME_FORMATTER)));
        sb.append("📍 Stops: [");
        
        if (stops.isEmpty()) {
            sb.append(" Empty ]");
        } else {
            for (int i = 0; i < stops.size(); i++) {
                RouteStop stop = stops.get(i);
                sb.append("\n  ").append(i + 1).append(". ").append(stop.toString());
            }
            sb.append("\n]");
        }
        
        return sb.toString();
    }
}

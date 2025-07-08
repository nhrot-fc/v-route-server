package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.List;

public record Route(String vehicleId, List<RouteStop> stops, LocalDateTime startTime) {
}

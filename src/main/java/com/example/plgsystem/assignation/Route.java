package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Route {
    private final String vehicleId;
    private final List<RouteStop> stops;
    private final LocalDateTime startTime;
}

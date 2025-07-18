package com.example.plgsystem.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;

public class ActionFactory {
    public static Action createDrivingAction(List<Position> path, double fuelConsumedGal, LocalDateTime startTime,
            LocalDateTime endTime) {
        return new Action(ActionType.DRIVE, startTime, endTime, path, 0, 0, fuelConsumedGal, 0.0, null, null, 0.0, false);
    }

    public static Action createRefuelingAction(String depotId, Position endPosition, LocalDateTime startTime, double refueledGal) {
        LocalDateTime endTime = startTime.plusMinutes(Constants.REFUEL_DURATION_MINUTES);
        return new Action(ActionType.REFUEL, startTime, endTime, List.of(endPosition), 0, 0, 0.0, refueledGal, null, depotId, 0.0, false);
    }

    public static Action createRefillingAction(String depotId, Position endPosition, LocalDateTime startTime, int glpAmountAdded) {
        LocalDateTime endTime = startTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
        return new Action(ActionType.RELOAD, startTime, endTime, List.of(endPosition), 0, glpAmountAdded, 0.0, 0.0, null, depotId, 0.0, false);
    }

    public static Action createServingAction(Position endPosition, String orderId, int glpDispensedM3, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        return new Action(ActionType.SERVE, startTime, endTime, List.of(endPosition), glpDispensedM3, 0, 0.0, 0.0, orderId, null, 0.0, false);
    }

    public static Action createIdleAction(Position endPosition, Duration duration, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plus(duration);
        return new Action(ActionType.WAIT, startTime, endTime, List.of(endPosition), 0, 0, 0.0, 0.0, null, null, 0.0, false);
    }

    public static Action createMaintenanceAction(Position endPosition, Duration duration, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plus(duration);
        return new Action(ActionType.MAINTENANCE, startTime, endTime, List.of(endPosition), 0, 0, 0.0, 0.0, null, null, 0.0, false);
    }
}
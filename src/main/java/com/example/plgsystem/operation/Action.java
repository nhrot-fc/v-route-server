package com.example.plgsystem.operation;

import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an action that a vehicle can execute during the simulation.
 * Actions include movements, loading, unloading, refueling, etc.
 */
@Getter
@AllArgsConstructor
public class Action {
    // General attributes for all actions
    private final ActionType type;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<Position> path;

    private final int glpDelivered;
    private final int glpLoaded;

    private final double fuelConsumedGal;
    private final double fuelRefueledGal;

    // Reference IDs to facilitate searches
    private final String orderId; // ID of the order being served, null for non-SERVE actions
    private final String depotId; // ID of the depot being used, null for non-REFUEL/RELOAD actions

    private double currentProgress; // 0 to 1 for execution progress monitoring
    
    @Override
    public String toString() {
        String emoji = getActionEmoji();
        String actionName = type.name();
        String progress = String.format("%.0f%%", currentProgress * 100);
        String timeInfo = String.format("⏱️ %s → %s", 
                startTime.format(Constants.DATE_TIME_FORMATTER), 
                endTime.format(Constants.DATE_TIME_FORMATTER));
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s %s { %s, ⏳ progress: %s", emoji, actionName, timeInfo, progress));
        
        switch (type) {
            case DRIVE:
                sb.append(String.format(", 🛣️ path: %d positions, ⛽ fuel: %.2f gal", 
                        path != null ? path.size() : 0, fuelConsumedGal));
                break;
            case REFUEL:
                sb.append(String.format(", 🏭 depot: %s, ⛽ refueled: %.2f gal", depotId, fuelRefueledGal));
                break;
            case RELOAD:
                sb.append(String.format(", 🏭 depot: %s, 🛢️ loaded: %d m³", depotId, glpLoaded));
                break;
            case SERVE:
                sb.append(String.format(", 🏪 order: %s, 🛢️ delivered: %d m³", orderId, glpDelivered));
                break;
            case MAINTENANCE:
                sb.append(", 🔧 maintenance operation");
                break;
            case WAIT:
                sb.append(", ⌛ waiting");
                break;
            default:
                break;
        }
        
        sb.append(" }");
        return sb.toString();
    }
    
    private String getActionEmoji() {
        switch (type) {
            case DRIVE: return "🚗";
            case REFUEL: return "⛽";
            case RELOAD: return "🔄";
            case SERVE: return "📦";
            case MAINTENANCE: return "🔧";
            case WAIT: return "⏳";
            default: return "❓";
        }
    }
}

package com.example.plgsystem.operation;

import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a plan of actions for a vehicle to execute during the simulation.
 */
@Getter
@AllArgsConstructor
public class VehiclePlan {
    private final String vehicleId;
    private final List<Action> actions;
    private final LocalDateTime startTime;

    private int currentActionIndex; // index of the current action to be executed

    public Action getCurrentAction() {
        if (currentActionIndex < 0 || currentActionIndex >= actions.size()) {
            return null;
        }
        return actions.get(currentActionIndex);
    }

    public double getCurrentActionProgress() {
        Action currentAction = getCurrentAction();
        if (currentAction == null) {
            return -1; // error
        }
        return currentAction.getCurrentProgress();
    }

    public void advanceAction() {
        currentActionIndex++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📝 VehiclePlan { 🚚 vehicleId: %s, 🕒 startTime: %s }\n",
                vehicleId, startTime.format(Constants.DATE_TIME_FORMATTER)));
        sb.append(String.format("🔄 Current Action: %d/%d\n", currentActionIndex + 1, actions.size()));
        sb.append("🗒️ Actions: [");

        if (actions.isEmpty()) {
            sb.append(" Empty ]");
        } else {
            for (int i = 0; i < actions.size(); i++) {
                Action action = actions.get(i);
                String prefix = (i == currentActionIndex) ? "▶️ " : "  ";
                sb.append("\n  ").append(prefix).append(i + 1).append(". ").append(action.toString());
            }
            sb.append("\n]");
        }

        return sb.toString();
    }

    public VehiclePlan copy() {
        List<Action> copiedActions = actions.stream()
                .map(Action::copy)
                .toList();
        return new VehiclePlan(vehicleId, copiedActions, startTime, currentActionIndex);
    }
}

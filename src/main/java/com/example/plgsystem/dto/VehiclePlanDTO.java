package com.example.plgsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.operation.VehiclePlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring VehiclePlan information to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePlanDTO {
    private String vehicleId;
    private List<ActionDTO> actions;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int currentActionIndex;

    /**
     * Converts a VehiclePlan entity to a VehiclePlanDTO
     * 
     * @param plan The VehiclePlan to convert
     * @return A VehiclePlanDTO representation of the plan
     */
    public static VehiclePlanDTO fromEntity(VehiclePlan plan) {
        if (plan == null) {
            return null;
        }

        LocalDateTime endTime = plan.getStartTime();
        if (plan.getActions() != null && !plan.getActions().isEmpty()) {
            endTime = plan.getActions().get(plan.getActions().size() - 1).getEndTime();
        }

        return VehiclePlanDTO.builder()
                .vehicleId(plan.getVehicleId())
                .actions(plan.getActions().stream()
                        .map(ActionDTO::fromEntity)
                        .toList())
                .startTime(plan.getStartTime())
                .endTime(endTime)
                .currentActionIndex(plan.getCurrentActionIndex())
                .build();
    }
}
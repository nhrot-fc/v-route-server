package com.example.plgsystem.dto;

import com.example.plgsystem.model.ServeRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServeRecordDTO {
    private UUID id;
    private String vehicleId;
    private String orderId;
    private int glpVolumeM3;
    private LocalDateTime serveDate;

    public static ServeRecordDTO fromEntity(ServeRecord serveRecord) {
        return ServeRecordDTO.builder()
                .id(serveRecord.getId())
                .vehicleId(serveRecord.getVehicle().getId())
                .orderId(serveRecord.getOrder().getId())
                .glpVolumeM3(serveRecord.getGlpVolumeM3())
                .serveDate(serveRecord.getServeDate())
                .build();
    }
}

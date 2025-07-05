package com.example.plgsystem.dto;

import com.example.plgsystem.model.ServeRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServeRecordDTO {
    private Long id;
    private String vehicleId;
    private String orderId;
    private int volumeM3;
    private LocalDateTime serveDate;
    
    // Conversión desde entidad a DTO
    public static ServeRecordDTO fromEntity(ServeRecord serveRecord) {
        return ServeRecordDTO.builder()
                .id(serveRecord.getId())
                .vehicleId(serveRecord.getVehicleId())
                .orderId(serveRecord.getOrderId())
                .volumeM3(serveRecord.getVolumeM3())
                .serveDate(serveRecord.getServeDate())
                .build();
    }
    
    // Conversión desde DTO a entidad
    public ServeRecord toEntity() {
        ServeRecord record = new ServeRecord(vehicleId, orderId, volumeM3, serveDate);
        // No podemos setear el ID directamente ya que es generado
        return record;
    }
}

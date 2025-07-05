package com.example.plgsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRecordDTO {
    private String vehicleId;
    private int volumeM3;
    private LocalDateTime serveDate;
}

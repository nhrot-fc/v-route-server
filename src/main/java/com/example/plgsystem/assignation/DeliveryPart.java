package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeliveryPart {
    private final String orderId;
    private final int glpDeliverM3;
    private final LocalDateTime deadlineTime;
}

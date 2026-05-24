package com.siscontrol.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CheckOutRequestDTO {
    private Long userId;
    private Long installationId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime deviceExitTime;
}
package com.siscontrol.backend.dto;

import lombok.Data;

@Data
public class CheckInRequestDTO {
    private Long userId;
    private Long installationId;
    private Double latitude;
    private Double longitude;
}
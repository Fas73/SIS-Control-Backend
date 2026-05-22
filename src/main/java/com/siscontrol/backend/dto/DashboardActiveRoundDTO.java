package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardActiveRoundDTO {
    private Long id;
    private String guardName;
    private String location;
    private float progreso;
    private String statusDisplay;
    private String status;
}
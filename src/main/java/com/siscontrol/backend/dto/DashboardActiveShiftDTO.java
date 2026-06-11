package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardActiveShiftDTO {
    private Long id;
    private String guardName;
    private String location;
    private String entryTime;
    private Double latitude;
    private Double longitude;
}
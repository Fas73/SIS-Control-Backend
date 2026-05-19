package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardDTO {
    private int totalGuards;
    private int activeShiftsCount;
    private int totalRoundsToday;
    private int roundsInProgress;
    private int roundsCompleted;
    private int totalIncidents;
    private int pendingIncidents;
    private int totalInstallations;
    private int activeInstallationsCount;
    private List<DashboardActiveRoundDTO> activeRoundsList;
    private List<DashboardActiveShiftDTO> activeShiftsList;
}
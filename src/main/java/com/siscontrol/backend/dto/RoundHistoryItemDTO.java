package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoundHistoryItemDTO {
    private Long id;
    private String installationName;
    private String startTime;
    private String endTime;
    private long durationMinutes;
    private String status;
    private String statusDisplay;
    private int checkpointsExecuted;
    private int checkpointsTotal;
    private int incidentCount;
    private String shiftStartTime;
    private String shiftEndTime;
    private String detailedSummary;
}
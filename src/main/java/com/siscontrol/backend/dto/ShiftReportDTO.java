package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiftReportDTO {
    private Long shiftId;
    private String workerName;
    private String installationName;
    
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime entryTime;
    
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime exitTime;
    
    private Integer totalRoundsPlanned;
    private Integer totalRoundsExecuted;
    private List<RoundDetailDTO> rondas;
    private List<IncidentDetailDTO> incidentes;
    private MetricsDTO metrics;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoundDetailDTO {
        private Long roundId;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;
        
        private String observations;
        private String status; // "COMPLETADA", "INCOMPLETA", "EN_PROGRESO"
        private List<ChecklogDetailDTO> checklogs;
        private List<IncidentDetailDTO> incidentes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChecklogDetailDTO {
        private String checkpointName;
        private Integer status;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime time;
        private String imageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IncidentDetailDTO {
        private Long id;
        private String title;
        private String description;
        private String severity;
        private String imageUrl;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
        private Integer status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricsDTO {
        private Integer totalCheckpoints;
        private Integer scannedCheckpoints;
        private Integer omittedCheckpoints;
        private Integer alertsCount;
    }
}

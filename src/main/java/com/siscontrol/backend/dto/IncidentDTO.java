package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDTO {
    private Long id;
    private String title;
    private String description;
    private String severity;
    private String imageUrl;
    private String type;
    private LocalDateTime createdAt;

    private Long roundExecutionId;
    private Long checklogId;
    private Integer status;

    // --- NUEVOS CAMPOS REQUERIDOS POR EL FRONTEND MÓVIL ---
    private String username;
    private String clientName;
    private String checkpointName;
    private Integer checkpointOrder;
    // METADATOS DE AUDITORÍA FOTOGRÁFICA CRUZADA
    private String checklogImageUrl;

    // METADATOS DE AUDITORÍA OFFLINE DESDE ROOM
    private java.time.LocalDateTime clientTimestamp;
    private Double latitude;
    private Double longitude;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private RoundExecutionSummaryDTO roundExecution;

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RoundExecutionSummaryDTO {
        private Long id;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;
    }
}
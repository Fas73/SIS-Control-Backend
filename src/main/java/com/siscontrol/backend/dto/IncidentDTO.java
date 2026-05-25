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

    // --- NUEVOS CAMPOS PARA ANALISIS IA GEMINI (MVP) ---
    private String descripcionOriginal;
    private String tipoIncidenteIA;
    private String prioridadIA;
    private String resumenIA;
    private String accionSugeridaIA;
    private Boolean requiereAtencionInmediata;
    private String estadoAnalisisIA;
    private LocalDateTime fechaAnalisisIA;
    private String modeloIA;
}
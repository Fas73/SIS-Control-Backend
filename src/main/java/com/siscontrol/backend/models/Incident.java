package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.siscontrol.backend.enums.IncidentType;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.RoundExecution;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // Requerido por Postman
    private String description;
    private String severity;    // Requerido por Postman (Ej: "Alta")

    @Column(columnDefinition = "TEXT")
    private String imageUrl;    // Requerido por Postman para fotos de evidencia

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private IncidentType type;

    @ManyToOne
    @JoinColumn(name = "round_execution_id", nullable = false)
    @JsonIgnoreProperties({"worker", "installation"}) // Evita bucle infinito
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checklog_id", nullable = true)
    private Checklog checklog;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer status = 0; // 0 = Reportado/Pendiente, 1 = Atendido/Resuelto

    // --- NUEVOS CAMPOS PARA ANALISIS IA GEMINI (MVP) ---
    @Column(columnDefinition = "TEXT")
    private String descripcionOriginal;

    private String tipoIncidenteIA;
    private String prioridadIA;

    @Column(columnDefinition = "TEXT")
    private String resumenIA;

    @Column(columnDefinition = "TEXT")
    private String accionSugeridaIA;

    private Boolean requiereAtencionInmediata;
    private String estadoAnalisisIA; // PENDIENTE, ANALIZADO, ERROR
    private LocalDateTime fechaAnalisisIA;
    private String modeloIA;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (descripcionOriginal == null) descripcionOriginal = description;
        if (estadoAnalisisIA == null) estadoAnalisisIA = "PENDIENTE";
    }
}
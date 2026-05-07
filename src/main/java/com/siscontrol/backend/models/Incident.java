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

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
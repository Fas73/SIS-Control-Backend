package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.siscontrol.backend.enums.IncidentType;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private IncidentType type;

    // --- CORRECCIÓN CRÍTICA: CAMBIADO A NULLABLE = TRUE ---
    @ManyToOne
    @JoinColumn(name = "round_execution_id", nullable = true)
    @JsonIgnoreProperties({"worker", "installation"})
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checklog_id", nullable = true)
    private Checklog checklog;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer status = 0;

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
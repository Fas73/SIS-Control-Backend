package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
// Esta anotación hará que cualquier campo nulo no aparezca en el JSON final
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String severity;

    private String imageUrl;

    private LocalDateTime createdAt;

    private String description;

    // Usamos el Enum definido arriba.
    // Al ser STRING, guardará el texto (ej: "ROBO") en la base de datos
    @Enumerated(EnumType.STRING)
    private IncidentType type;

    @ManyToOne
    @JoinColumn(name = "round_execution_id", nullable = false)
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checklog_id", nullable = true)
    private Checklog checklog;

    // El método pre-persist es una forma excelente de asegurar
    // que la fecha se asigne automáticamente al guardar
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
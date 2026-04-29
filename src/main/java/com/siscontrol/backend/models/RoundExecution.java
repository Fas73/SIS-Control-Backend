package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "round_executions")
@Data
// 1. Cambiamos "round" por "installation" en el orden del JSON
@JsonPropertyOrder({ "id", "installation", "status", "startTime", "endTime" })
public class RoundExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. Cambiamos el tipo de dato y la relación
    @ManyToOne
    @JoinColumn(name = "installation_id") // Cambiamos el nombre de la columna en BD
    private Installation installation; // Cambiamos el nombre de la variable

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoundStatus status;
}
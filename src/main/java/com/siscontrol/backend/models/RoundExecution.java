package com.siscontrol.backend.models;

import com.siscontrol.backend.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "round_executions")
@Data
public class RoundExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    // Campo unificado para comentarios finales u observaciones
    private String observations;
}
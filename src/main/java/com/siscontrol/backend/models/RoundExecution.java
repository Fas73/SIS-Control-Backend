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

    @Column(name = "start_time", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime startTime;

    @Column(name = "end_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime endTime;

    @Column(name = "device_start_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime deviceStartTime;

    @Column(name = "device_end_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime deviceEndTime;

    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    // Campo unificado para comentarios finales u observaciones
    private String observations;
}
package com.siscontrol.backend.models;

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

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // Ej: "IN_PROGRESS", "COMPLETED"

    @ManyToOne
    @JoinColumn(name = "round_id")
    private Round round;
}
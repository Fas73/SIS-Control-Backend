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

    // 2. Cambiamos el tipo de dato y la relación
    @ManyToOne
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne
    @JoinColumn(name = "guard_id", nullable = false)
    private User guard;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoundStatus status;

}
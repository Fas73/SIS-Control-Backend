package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.siscontrol.backend.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "round_executions")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "guard", "installation", "status", "startTime", "endTime", "cancellationReason" })
public class RoundExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "guard_id")
    private User guard;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoundStatus status;

    // Nuevo campo para auditoría de cancelaciones
    private String cancellationReason;
}
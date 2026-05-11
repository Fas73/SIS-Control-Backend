package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.models.RoundExecution;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklogs")
@Data
public class Checklog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "execution_id")
    @JsonIgnoreProperties({"worker", "installation"})
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checkpoint_id")
    @JsonIgnoreProperties({"installation"}) // Importante: No repetir la instalación
    private Checkpoint checkpoint;
}
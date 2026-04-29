package com.siscontrol.backend.models;

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
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checkpoint_id")
    private Checkpoint checkpoint;
}
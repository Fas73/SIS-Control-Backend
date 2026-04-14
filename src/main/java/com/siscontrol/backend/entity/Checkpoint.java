package com.siscontrol.backend.entity;


import com.siscontrol.backend.enums.CheckpointType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installation_id", nullable = false)
    private Installation installation;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckpointType type;

    @Column(name = "tag_identifier", nullable = false, unique = true, length = 255)
    private String tagIdentifier;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;
}

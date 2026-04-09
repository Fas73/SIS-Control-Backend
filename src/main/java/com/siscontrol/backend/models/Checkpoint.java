package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "checkpoints")
@Data
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String locationDescription;
    private String nfcTagCode;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;
}
package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "rounds")
@Data
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;
}
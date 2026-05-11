package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import com.siscontrol.backend.enums.UserStatus;

@Entity
@Table(name = "checkpoints")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String locationDescription;
    private String nfcTagCode;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    // Evita que la instalación intente listar sus propios checkpoints de vuelta
    @JsonIgnoreProperties({"checkpoints", "address", "clientName"})
    private Installation installation;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE; // Agrega esta línea
}
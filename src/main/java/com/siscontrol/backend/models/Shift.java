package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.siscontrol.backend.enums.ShiftStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "shifts")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Oculta campos nulos en la respuesta
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User worker;

    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    @Enumerated(EnumType.STRING)
    private ShiftStatus status;
}
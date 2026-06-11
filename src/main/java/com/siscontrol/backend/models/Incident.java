package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.siscontrol.backend.enums.IncidentType;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT") // Esto le dice a Hibernate que es un campo largo
    private String description;

    private String severity;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private IncidentType type;

    @ManyToOne(fetch = FetchType.LAZY) // Evita que Hibernate inspeccione los escaneos de la ronda
    @JoinColumn(name = "round_execution_id", nullable = true)
    @JsonIgnoreProperties({"worker", "installation"})
    private RoundExecution roundExecution;

    @ManyToOne(fetch = FetchType.LAZY) // Evita inspecciones fantasmas en la tabla checklogs
    @JoinColumn(name = "checklog_id", nullable = true)
    private Checklog checklog;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer status = 0;

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    // ADICIONES PARA AUDITORÍA DE MARCA DE AGUA (WATERMARK)
    @Column(name = "client_timestamp")
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm[:ss][.SSS][.SS][.S]")
    private LocalDateTime clientTimestamp; // Capturado por el reloj/GPS del celular del guardia

    @Column(name = "latitude")
    private Double latitude; // Sensor de latitud (flotante nativo compatible con Hibernate 7)

    @Column(name = "longitude")
    private Double longitude; // Sensor de longitud

    @Column(name = "shift_id", nullable = true)
    private Long shiftId;
}
package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkpoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Checkpoint extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private Integer executionOrder; //

    @Column(unique = true, nullable = false)
    private String nfcTagCode;

    private String locationDescription;

    @Column(columnDefinition = "TEXT")
    private String instruction; //

    @ManyToOne
    @JoinColumn(name = "installation_id")
    private Installation installation;

    private Integer status = 1;

    // NUEVOS CAMPOS: Configuración para el flujo inteligente de capturas en la App Móvil
    @Column(name = "requires_photo", nullable = false)
    private boolean requiresPhoto = false; // Por defecto el punto de control no exige foto

    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl; // URL de la fotografía maestra para auditorías de IA

}
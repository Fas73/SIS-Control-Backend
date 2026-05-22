package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "installations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Installation extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String clientName;

    // Campos específicos para GPS
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // Radio de tolerancia en metros (por defecto 100m)
    private Double radiusInMeters = 100.0;

    private Integer status = 1;
}
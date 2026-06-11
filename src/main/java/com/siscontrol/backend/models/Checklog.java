package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Checklog extends Auditable { // Ahora sí encuentra Auditable gracias al package común

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "scanned_at", nullable = false)
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm[:ss][.SSS][.SS][.S]")
    private LocalDateTime scannedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonIgnoreProperties({"worker", "installation", "checklogs"})
    private RoundExecution roundExecution; // Resuelto por el compilador

    @ManyToOne
    @JoinColumn(name = "checkpoint_id", nullable = false)
    @JsonIgnoreProperties({"installation", "status"})
    private Checkpoint checkpoint; // Resuelto por el compilador

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer status = 1;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    // === METADATOS TRANSITORIOS BLINDADOS PARA EVITAR EL CHOQUE DE DESERIALIZACIÓN ===
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long roundExecutionId;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long checkpointId;
}
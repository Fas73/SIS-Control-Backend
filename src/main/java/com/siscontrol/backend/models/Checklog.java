package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // Para incluir los campos de auditoría en equals/hashCode
public class Checklog extends Auditable { // HEREDA DE AUDITABLE para trazar quién registró

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sustituimos 'timestamp' por scannedAt para ser más descriptivos
    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonIgnoreProperties({"worker", "installation", "checklogs"})
    private RoundExecution roundExecution;

    @ManyToOne
    @JoinColumn(name = "checkpoint_id", nullable = false)
    @JsonIgnoreProperties({"installation", "status"})
    private Checkpoint checkpoint;

    // NUEVO CAMPO: Aquí es donde se guarda el comentario de Postman
    @Column(length = 500)
    private String notes;

    // Estado del escaneo (1 = Válido, 0 = Anulado/Impugnado)
    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer status = 1;
}
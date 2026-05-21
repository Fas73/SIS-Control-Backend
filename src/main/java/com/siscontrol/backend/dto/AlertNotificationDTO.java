package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertNotificationDTO {
    private Long id;
    private String title;          // "Checkpoint no escaneado" o "🚨 BOTÓN DE PÁNICO ACTIVADO"
    private String description;    // Justificación/Detalle
    private String severity;       // "Alta", "Media", "Baja"
    private String type;           // Tipo de incidente
    private LocalDateTime createdAt;
    private Integer status;
    private Long roundExecutionId;
    private String username;       // Nombre del guardia que ejecuta la ronda

    // --- LOS DATOS CRUZADOS QUE SOLICITAS ---
    private String checkpointName;     // de la tabla checkpoints
    private Integer executionOrder;    // de la tabla checkpoints
    private String clientName;         // de la tabla installations
}
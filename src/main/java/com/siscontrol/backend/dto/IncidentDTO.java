package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDTO {
    private Long id;
    private String title;
    private String description;
    private String severity;
    private String imageUrl;
    private String type; // Campo para el Enum
    private LocalDateTime createdAt;

    // Campos para las relaciones
    private Long roundExecutionId;
    private Long checklogId; // Puede ser null
}
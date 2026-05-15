package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String message;
    private boolean success;
    private Long userId;
    private String username;
    private String role;
    private String token;    // Agregado para el Frontend
    private String fullName; // Agregado para el Frontend
}
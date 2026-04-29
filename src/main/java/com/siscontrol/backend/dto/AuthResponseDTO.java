package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String message;
    private boolean success;
    private String username;
    private String role;
}
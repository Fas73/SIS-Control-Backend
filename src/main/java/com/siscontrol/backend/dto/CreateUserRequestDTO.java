package com.siscontrol.backend.dto;

import com.siscontrol.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequestDTO {
    private String rut;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private UserRole role;

    // NUEVO CAMPO: Para capturar la selfie de enrolamiento procesada por el teléfono
    private String profileImageUrl;
}
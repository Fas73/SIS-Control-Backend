package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String rut;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Integer status;
    private String createdAt;
}
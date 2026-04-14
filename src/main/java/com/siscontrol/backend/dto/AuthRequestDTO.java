package com.siscontrol.backend.dto;

import lombok.Data;

@Data
public class AuthRequestDTO {
    private String username;
    private String password;
}
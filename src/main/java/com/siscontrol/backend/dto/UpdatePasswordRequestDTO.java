package com.siscontrol.backend.dto;

import lombok.Data;

@Data
public class UpdatePasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
}
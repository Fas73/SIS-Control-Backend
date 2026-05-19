package com.siscontrol.backend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    private String fullName;
    private String username;
    private String phoneNumber;
}
package com.siscontrol.backend.dto;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequestDTO {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private UserRole role;
    private UserStatus status;
}
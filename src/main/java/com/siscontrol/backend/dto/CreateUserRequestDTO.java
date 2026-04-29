package com.siscontrol.backend.dto;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;


public class CreateUserRequestDTO {

    private String username;
    private String email;
    private String password;
    private String fullName;
    private UserRole role;
    private UserStatus status;

    public CreateUserRequestDTO() {
    }

    public CreateUserRequestDTO(String username, String email, String password, String fullName, UserRole role, UserStatus status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

}

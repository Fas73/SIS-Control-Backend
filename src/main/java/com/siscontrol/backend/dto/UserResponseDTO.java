package com.siscontrol.backend.dto;


public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String createdAt;

    public UserResponseDTO(Long id, String username, String email, String fullName, String role, String status, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }



}

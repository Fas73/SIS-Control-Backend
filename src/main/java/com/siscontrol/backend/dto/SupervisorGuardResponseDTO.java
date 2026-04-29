package com.siscontrol.backend.dto;


public class SupervisorGuardResponseDTO {
    private UserResponseDTO supervisor;
    private UserResponseDTO guard;

    public SupervisorGuardResponseDTO(UserResponseDTO supervisor, UserResponseDTO guard) {
        this.supervisor = supervisor;
        this.guard = guard;
    }

    public UserResponseDTO getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(UserResponseDTO supervisor) {
        this.supervisor = supervisor;
    }

    public UserResponseDTO getGuard() {
        return guard;
    }

    public void setGuard(UserResponseDTO guard) {
        this.guard = guard;
    }

}



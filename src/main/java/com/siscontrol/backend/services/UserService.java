package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserStatus;
import com.siscontrol.backend.exception.BadRequestException;
import com.siscontrol.backend.exception.ForbiddenException;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }

        return Optional.empty();
    }

    public UserResponseDTO convertirAResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }

    public UserResponseDTO crearUsuario(Long adminId, CreateUserRequestDTO request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        if (!admin.getRole().name().equals("ADMIN")) {
            throw new ForbiddenException("solo los admin pueden crear usuarios");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("el username es obligatorio");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("el email es obligatorio");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("la contraseña es obligatoria");
        }

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("el nombre completo es obligatorio");
        }

        if (request.getRole() == null) {
            throw new BadRequestException("el rol es obligatorio");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("el username ya existe");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("el email ya existe");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());
        newUser.setFullName(request.getFullName());
        newUser.setRole(request.getRole());
        newUser.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);

        User savedUser = userRepository.save(newUser);
        return convertirAResponseDTO(savedUser);

       

    }


    // public User guardarUsuario(User user) {
    //     return userRepository.save(user);
    // }

    // public UserResponseDTO convertirAResponseDTO(User user) {
    //     return new UserResponseDTO(
    //             user.getId(),
    //             user.getUsername(),
    //             user.getEmail(),
    //             user.getFullName(),
    //             user.getRole().name(),
    //             user.getStatus().name(),
    //             user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
    //     );
    // }
}
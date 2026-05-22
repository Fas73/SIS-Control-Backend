package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.AuthRequestDTO;
import com.siscontrol.backend.dto.AuthResponseDTO;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        Optional<User> user = userService.login(request.getUsername(), request.getPassword());

        if (user.isPresent()) {
            User u = user.get();
            // Retornamos todos los datos, incluido el ID que Android necesita guardar
            return ResponseEntity.ok(
                    new AuthResponseDTO(
                            "Login exitoso",
                            true,
                            u.getId(),         // ID para usar en @RequestParam
                            u.getUsername(),
                            u.getRole().name(),
                            "DUMMY_TOKEN_123",
                            u.getFullName()
                    )
            );
        }

        // Si falla, devolvemos 401 con los campos en null/false
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new AuthResponseDTO(
                        "Credenciales inválidas",
                        false,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );
    }
}
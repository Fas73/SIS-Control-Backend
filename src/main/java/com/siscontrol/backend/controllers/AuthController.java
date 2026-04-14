package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.AuthRequestDTO;
import com.siscontrol.backend.dto.AuthResponseDTO;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        Optional<User> user = userService.login(request.getUsername(), request.getPassword());

        if (user.isPresent()) {
            return ResponseEntity.ok(
                    new AuthResponseDTO(
                            "Login exitoso",
                            true,
                            user.get().getUsername(),
                            user.get().getRole().name()
                    )
            );
        }

        return ResponseEntity.status(401).body(
                new AuthResponseDTO(
                        "Credenciales inválidas",
                        false,
                        null,
                        null
                )
        );
    }
}
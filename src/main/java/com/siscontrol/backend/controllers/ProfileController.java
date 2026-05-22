package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.UpdateProfileRequestDTO;
import com.siscontrol.backend.dto.UpdatePasswordRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserService userService;

    // CAPTURA 1: Guardar cambios del perfil (Cualquier usuario)
    @PutMapping("/{userId}/datos")
    public ResponseEntity<UserResponseDTO> editarMiPerfil(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(userService.actualizarMisDatos(userId, request));
    }

    // CAPTURA 2: Cambiar contraseña desde la app móvil (Cualquier usuario)
    @PutMapping("/{userId}/contrasena")
    public ResponseEntity<?> cambiarMiContrasena(
            @PathVariable Long userId,
            @RequestBody UpdatePasswordRequestDTO request) {
        userService.actualizarMiContrasena(userId, request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente."));
    }
}
package com.siscontrol.backend.controllers;

import java.util.Map;
import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserResponseDTO> crearUsuario(
            @RequestParam Long creatorId,
            @RequestBody CreateUserRequestDTO request) {
        return new ResponseEntity<>(userService.crearUsuario(creatorId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @RequestParam Long editorId,
            @RequestBody CreateUserRequestDTO request) {
        return ResponseEntity.ok(userService.actualizarUsuario(editorId, id, request));
    }

    // --- NUEVO ENDPOINT: ACTUALIZAR FOTO DE PERFIL DESDE ANDROID ---
    // PATCH http://localhost:8080/api/usuarios/9/profile-image?url=https://...
    @PatchMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDTO> actualizarFotoPerfil(
            @PathVariable Long id,
            @RequestParam String url) {

        UserResponseDTO actualizado = userService.actualizarFotoPerfil(id, url);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Long editorId,
            @RequestParam Integer status) {
        return ResponseEntity.ok(userService.cambiarEstado(editorId, id, status));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponseDTO> toggleStatus(
            @PathVariable Long id,
            @RequestParam Long editorId) {

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        Integer nuevoEstado = (targetUser.getStatus() == 1) ? 0 : 1;

        return ResponseEntity.ok(userService.cambiarEstado(editorId, id, nuevoEstado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(
            @PathVariable Long id,
            @RequestParam Long editorId) {
        userService.eliminarUsuario(editorId, id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado correctamente (status 0)"));
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        return ResponseEntity.ok(userService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerPorId(id));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<?> obtenerPorRol(@PathVariable String role) {
        try {
            UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
            return ResponseEntity.ok(userService.obtenerPorRol(roleEnum));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    Map.of("error", "El rol '" + role + "' no es válido. Use ADMIN, SUPERVISOR o GUARD."),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
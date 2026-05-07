package com.siscontrol.backend.controllers;

import java.util.Map;
import java.util.List;
import com.siscontrol.backend.enums.UserStatus;
import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserRole;
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

    @PostMapping
    public ResponseEntity<UserResponseDTO> crearUsuario(@RequestParam Long creatorId, @RequestBody CreateUserRequestDTO request) {
        return new ResponseEntity<>(userService.crearUsuario(creatorId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> actualizarUsuario(@PathVariable Long id, @RequestParam Long editorId, @RequestBody CreateUserRequestDTO request) {
        return ResponseEntity.ok(userService.actualizarUsuario(editorId, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam Long editorId, @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.cambiarEstado(editorId, id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable Long id, @RequestParam Long editorId) {
        userService.eliminarUsuario(editorId, id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado correctamente"));
    }

    // --- CONSULTAS CON MENSAJES DE LISTA VACÍA ---

    // URL: http://localhost:8080/api/usuarios?requesterId=1
    @GetMapping
    public ResponseEntity<?> listarTodos(@RequestParam Long requesterId) {
        // Usamos List<?> para evitar el error de tipos incompatibles si el Service devuelve Object o List genérica
        Object resultado = userService.listarTodos();

        if (resultado instanceof List) {
            List<?> lista = (List<?>) resultado;
            if (lista.isEmpty()) {
                return ResponseEntity.ok(Map.of("mensaje", "No hay usuarios registrados en el sistema."));
            }
        }

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerPorId(id));
    }

    // URL: http://localhost:8080/api/usuarios/role/supervisor
    @GetMapping("/role/{role}")
    public ResponseEntity<?> obtenerPorRol(@PathVariable String role) {
        try {
            UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
            Object resultado = userService.obtenerPorRol(roleEnum);

            if (resultado instanceof List) {
                List<?> lista = (List<?>) resultado;
                if (lista.isEmpty()) {
                    return ResponseEntity.ok(Map.of("mensaje", "No se encontraron usuarios con el rol: " + role.toUpperCase()));
                }
            }
            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("error", "El rol '" + role + "' no es válido."), HttpStatus.BAD_REQUEST);
        }
    }
}
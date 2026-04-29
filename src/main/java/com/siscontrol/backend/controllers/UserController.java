package com.siscontrol.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.services.UserService;
import com.siscontrol.backend.exception.BadRequestException;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> crearUsuario(@RequestParam Long creatorId, @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO nuevoUsuario = userService.crearUsuario(creatorId, request);
        return ResponseEntity.ok(nuevoUsuario);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listarTodos() {
        return ResponseEntity.ok(userService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerPorId(id));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponseDTO>> obtenerPorRol(@PathVariable String role) {
        UserRole userRole;

        try {
            userRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Rol inválido. Valores permitidos: ADMIN, SUPERVISOR, GUARD");
        }

        return ResponseEntity.ok(userService.obtenerPorRol(userRole));
    }

    

}

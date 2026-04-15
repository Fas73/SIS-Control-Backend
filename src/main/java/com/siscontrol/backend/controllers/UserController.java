package com.siscontrol.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> crearUsuario(@RequestBody User user) {
        User nuevoUsuario = userService.guardarUsuario(user);
        UserResponseDTO response = userService.convertirAResponseDTO(nuevoUsuario);
        return ResponseEntity.ok(response);
    }

    

}

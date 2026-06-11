package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.AuthRequestDTO;
import com.siscontrol.backend.dto.AuthResponseDTO;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.repositories.UserRepository;
import com.siscontrol.backend.services.UserService;
import com.siscontrol.backend.services.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertService alertService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        Optional<User> user = userService.login(request.getUsername(), request.getPassword());

        if (user.isPresent()) {
            User u = user.get();
            return ResponseEntity.ok(
                    new AuthResponseDTO(
                            "Login exitoso",
                            true,
                            u.getId(),
                            u.getUsername(),
                            u.getRole().name(),
                            "DUMMY_TOKEN_123",
                            u.getFullName()
                    )
            );
        }

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

    @PostMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody Map<String, String> requestBody) {
        String identifier = requestBody.get("username");
        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("exists", false));
        }
        boolean exists = userRepository.findByUsernameOrEmail(identifier, identifier).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // --- ENDPOINT CORREGIDO PARA LOGRAR COMPATIBILIDAD ABSOLUTA ---
    // POST http://localhost:8080/api/auth/recuperar-acceso
    // Cuerpo JSON: { "email": "juan@correo.com" }
    @PostMapping("/recuperar-acceso")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> requestBody) {

        // Extraemos de forma segura el email desde el cuerpo JSON
        String email = requestBody.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El campo 'email' es obligatorio en el cuerpo de la solicitud."));
        }

        // 1. Validar si el correo existe en la base de datos
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El correo ingresado no se encuentra registrado en el sistema."));
        }

        User usuario = userOpt.get();

        // 2. Construir la alerta global sin ronda asociada
        Incident solicitud = new Incident();
        solicitud.setTitle("SOLICITUD DE ACCESO");
        solicitud.setDescription("El usuario " + usuario.getFullName() + " (RUT: " + usuario.getRut() + ") solicita restablecer su contraseña. Correo: " + email);
        solicitud.setSeverity("Alta");
        solicitud.setStatus(0);
        solicitud.setType(com.siscontrol.backend.enums.IncidentType.OTRO);
        solicitud.setCreatedAt(LocalDateTime.now());
        solicitud.setRoundExecution(null);
        solicitud.setChecklog(null);

        // 3. Guardar en la BD e irradiar en vivo por WebSockets
        alertService.registrarYDispararAlerta(solicitud);

        return ResponseEntity.ok(Map.of("mensaje", "Solicitud de asistencia enviada al Administrador con éxito."));
    }
}
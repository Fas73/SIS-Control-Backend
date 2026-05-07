package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;
import com.siscontrol.backend.exception.BadRequestException;
import com.siscontrol.backend.exception.ForbiddenException;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                if (user.getStatus() != UserStatus.ACTIVE) {
                    throw new ForbiddenException("El usuario está inactivo. Contacte al administrador.");
                }
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public UserResponseDTO crearUsuario(Long creatorId, CreateUserRequestDTO request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        validarPermisos(creator, request.getRole());
        validarCampos(request);

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("El username ya existe");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("El email ya existe");
        }

        User newUser = new User();
        copiarDatos(newUser, request);
        return convertirAResponseDTO(userRepository.save(newUser));
    }

    public UserResponseDTO actualizarUsuario(Long editorId, Long userId, CreateUserRequestDTO request) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario a editar no encontrado"));

        validarPermisos(editor, targetUser.getRole());

        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> { if(!u.getId().equals(userId)) throw new BadRequestException("Username en uso"); });

        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> { if(!u.getId().equals(userId)) throw new BadRequestException("Email en uso"); });

        targetUser.setFullName(request.getFullName());
        targetUser.setEmail(request.getEmail());
        targetUser.setUsername(request.getUsername());
        targetUser.setRole(request.getRole());
        targetUser.setStatus(request.getStatus() != null ? request.getStatus() : targetUser.getStatus());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            targetUser.setPassword(request.getPassword());
        }

        return convertirAResponseDTO(userRepository.save(targetUser));
    }

    public void eliminarUsuario(Long editorId, Long userId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        validarPermisos(editor, targetUser.getRole());
        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);
    }

    public UserResponseDTO cambiarEstado(Long editorId, Long userId, UserStatus nuevoEstado) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        validarPermisos(editor, targetUser.getRole());
        targetUser.setStatus(nuevoEstado);
        return convertirAResponseDTO(userRepository.save(targetUser));
    }

    // --- MÉTODOS DE CONSULTA CON MENSAJE DE VACÍO ---
    public Object listarTodos() {
        List<UserResponseDTO> lista = userRepository.findAll().stream().map(this::convertirAResponseDTO).toList();
        return validarLista(lista, "No existen usuarios registrados.");
    }

    public Object obtenerPorRol(UserRole role) {
        List<UserResponseDTO> lista = userRepository.findByRole(role).stream().map(this::convertirAResponseDTO).toList();
        return validarLista(lista, "No existen usuarios con el rol: " + role);
    }

    private Object validarLista(List<?> lista, String mensaje) {
        if (lista.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", 0);
            res.put("mensaje", mensaje);
            return res;
        }
        return lista;
    }

    public UserResponseDTO obtenerPorId(Long id) {
        return convertirAResponseDTO(userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No encontrado")));
    }

    // --- AUXILIARES ---
    private void validarPermisos(User creator, UserRole targetRole) {
        boolean isAdmin = creator.getRole() == UserRole.ADMIN;
        boolean isSupervisorManagingGuard = creator.getRole() == UserRole.SUPERVISOR && targetRole == UserRole.GUARD;
        if (!isAdmin && !isSupervisorManagingGuard) throw new ForbiddenException("No tienes permisos.");
    }

    private void copiarDatos(User user, CreateUserRequestDTO request) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);
    }

    private void validarCampos(CreateUserRequestDTO request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) throw new BadRequestException("Username obligatorio");
        if (request.getEmail() == null || request.getEmail().isBlank()) throw new BadRequestException("Email obligatorio");
        if (request.getPassword() == null || request.getPassword().isBlank()) throw new BadRequestException("Password obligatoria");
        if (request.getRole() == null) throw new BadRequestException("Rol obligatorio");
    }

    public UserResponseDTO convertirAResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
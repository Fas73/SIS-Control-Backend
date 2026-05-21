package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CreateUserRequestDTO;
import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.exception.BadRequestException;
import com.siscontrol.backend.exception.ForbiddenException;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                if (user.getStatus() != 1) {
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

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password obligatoria para la creación de un usuario");
        }

        if (userRepository.findByRut(request.getRut().toUpperCase()).isPresent()) {
            throw new BadRequestException("El RUT ya está registrado.");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("El username ya existe");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("El email ya existe");
        }

        User newUser = new User();
        copiarDatos(newUser, request);

        newUser.setCreatedBy(creatorId);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setStatus(1);

        return convertirAResponseDTO(userRepository.save(newUser));
    }

    public UserResponseDTO actualizarUsuario(Long editorId, Long userId, CreateUserRequestDTO request) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario a editar no encontrado"));

        validarPermisos(editor, targetUser.getRole());
        validarCampos(request);

        userRepository.findByRut(request.getRut().toUpperCase())
                .ifPresent(u -> { if(!u.getId().equals(userId)) throw new BadRequestException("El RUT ya pertenece a otro usuario."); });

        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> { if(!u.getId().equals(userId)) throw new BadRequestException("Username en uso"); });

        targetUser.setRut(request.getRut().toUpperCase());
        targetUser.setFullName(request.getFullName());
        targetUser.setEmail(request.getEmail());
        targetUser.setUsername(request.getUsername());
        targetUser.setRole(request.getRole());
        targetUser.setPhoneNumber(request.getPhoneNumber());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty() && !request.getPassword().equals("password123")) {
            targetUser.setPassword(request.getPassword());
        }

        targetUser.setUpdatedBy(editorId);
        targetUser.setUpdatedAt(LocalDateTime.now());

        return convertirAResponseDTO(userRepository.save(targetUser));
    }

    // --- NUEVO MÉTODO: ACTUALIZACIÓN EXCLUSIVA DE IMAGEN DESDE EL MOVIL ---
    @Transactional
    public UserResponseDTO actualizarFotoPerfil(Long userId, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        user.setProfileImageUrl(url);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(userId); // Se actualizó a sí mismo

        return convertirAResponseDTO(userRepository.save(user));
    }

    public void eliminarUsuario(Long editorId, Long userId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validarPermisos(editor, targetUser.getRole());

        targetUser.setStatus(0);
        targetUser.setUpdatedBy(editorId);
        targetUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(targetUser);
    }

    public UserResponseDTO cambiarEstado(Long editorId, Long userId, Integer nuevoEstado) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validarPermisos(editor, targetUser.getRole());

        if (nuevoEstado != 0 && nuevoEstado != 1) {
            throw new BadRequestException("El estado debe ser 1 (Activo) o 0 (Inactivo)");
        }

        targetUser.setStatus(nuevoEstado);
        targetUser.setUpdatedBy(editorId);

        return convertirAResponseDTO(userRepository.save(targetUser));
    }

    public Object listarTodos() {
        List<UserResponseDTO> lista = userRepository.findAll().stream()
                .map(this::convertirAResponseDTO)
                .toList();
        return validarLista(lista, "No existen usuarios registrados.");
    }

    public Object obtenerPorRol(UserRole role) {
        List<UserResponseDTO> lista = userRepository.findByRole(role).stream()
                .map(this::convertirAResponseDTO)
                .toList();
        return validarLista(lista, "No existen usuarios con el rol: " + role);
    }

    private void validarPermisos(User creator, UserRole targetRole) {
        boolean isAdmin = creator.getRole() == UserRole.ADMIN;
        boolean isSupervisorManagingGuard = creator.getRole() == UserRole.SUPERVISOR && targetRole == UserRole.GUARD;
        if (!isAdmin && !isSupervisorManagingGuard) throw new ForbiddenException("No tienes permisos para gestionar este tipo de usuario.");
    }

    private void copiarDatos(User user, CreateUserRequestDTO request) {
        user.setRut(request.getRut().toUpperCase());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setStatus(1);
    }

    private void validarCampos(CreateUserRequestDTO request) {
        if (request.getRut() == null || !request.getRut().matches("^[0-9]{7,8}-[0-9Kk]$")) {
            throw new BadRequestException("RUT inválido. Formato: xxxxxxxx-y (sin puntos, con guion)");
        }
        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("^\\+56\\d{9}$")) {
            throw new BadRequestException("El teléfono debe tener el formato +56 seguido de 9 dígitos (Ej: +56912345678)");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) throw new BadRequestException("Username obligatorio");
        if (request.getEmail() == null || request.getEmail().isBlank()) throw new BadRequestException("Email obligatorio");
        if (request.getRole() == null) throw new BadRequestException("Rol obligatorio");
    }

    public UserResponseDTO obtenerPorId(Long id) {
        return convertirAResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado")));
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

    // --- CORREGIDO: AHORA SÍ RETORNA LA URL DE LA IMAGEN EN EL DTO ---
    public UserResponseDTO convertirAResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getRut(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                user.getProfileImageUrl() // <-- ESTA ERA LA PIEZA QUE FALTABA MAPEAR
        );
    }

    @Transactional
    public UserResponseDTO actualizarMisDatos(Long userId, com.siscontrol.backend.dto.UpdateProfileRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("^\\+56\\d{9}$")) {
            throw new BadRequestException("El teléfono debe tener el formato +56 seguido de 9 dígitos (Ej: +56912345678)");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            userRepository.findByUsername(request.getUsername())
                    .ifPresent(u -> {
                        if(!u.getId().equals(userId)) throw new BadRequestException("El nombre de usuario ya está en uso.");
                    });
            user.setUsername(request.getUsername());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        user.setPhoneNumber(request.getPhoneNumber());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(userId);

        return convertirAResponseDTO(userRepository.save(user));
    }

    @Transactional
    public void actualizarMiContrasena(Long userId, com.siscontrol.backend.dto.UpdatePasswordRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getPassword().equals(request.getCurrentPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BadRequestException("La nueva contraseña no puede estar vacía.");
        }

        user.setPassword(request.getNewPassword());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(userId);

        userRepository.save(user);
    }
}
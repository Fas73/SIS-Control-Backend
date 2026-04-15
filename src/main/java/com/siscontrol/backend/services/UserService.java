package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.UserResponseDTO;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }

        return Optional.empty();
    }

    public User guardarUsuario(User user) {
        return userRepository.save(user);
    }

    public UserResponseDTO convertirAResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
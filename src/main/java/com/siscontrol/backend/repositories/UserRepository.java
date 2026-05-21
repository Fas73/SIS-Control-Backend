package com.siscontrol.backend.repositories;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Metodo para buscar si el identificador coincide con la columna username O email
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByRut(String rut); // Búsqueda por RUT
    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
}
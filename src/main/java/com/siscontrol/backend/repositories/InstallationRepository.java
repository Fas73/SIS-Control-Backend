package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InstallationRepository extends JpaRepository<Installation, Long> {
    // Para evitar instalaciones duplicadas por nombre
    Optional<Installation> findByNameIgnoreCase(String name);
}
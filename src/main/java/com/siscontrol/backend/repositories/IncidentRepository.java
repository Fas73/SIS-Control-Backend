package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    // Método para buscar incidentes asociados a una ronda específica
    List<Incident> findByRoundExecutionId(Long roundExecutionId);
}
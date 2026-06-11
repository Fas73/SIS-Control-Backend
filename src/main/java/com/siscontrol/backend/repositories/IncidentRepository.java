package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Metodo para buscar incidentes asociados a una ronda específica
    List<Incident> findByRoundExecutionId(Long roundExecutionId); //
    List<Incident> findByRoundExecutionIdIn(List<Long> roundExecutionIds);
    List<Incident> findByShiftId(Long shiftId);

    // Buscar incidente por el ID del checklog asociado
    java.util.Optional<Incident> findByChecklogId(Long checklogId);

    // NUEVA CONSULTA OPTIMIZADA: Une las relaciones en una sola transacción SQL
    @Query("SELECT i FROM Incident i LEFT JOIN FETCH i.roundExecution re LEFT JOIN FETCH re.installation LEFT JOIN FETCH re.worker LEFT JOIN FETCH i.checklog c LEFT JOIN FETCH c.checkpoint")
    List<Incident> findAllOptimized();
}
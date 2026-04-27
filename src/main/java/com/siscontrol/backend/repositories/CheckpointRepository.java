package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    // Esto crea la consulta SQL automáticamente, evitando traer toda la tabla a memoria
    List<Checkpoint> findByInstallationId(Long installationId);
}
package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    List<Checkpoint> findByInstallationId(Long installationId);

    // Necesario para validar que un Tag NFC no se use en dos lados
    Optional<Checkpoint> findByNfcTagCode(String nfcTagCode);
}
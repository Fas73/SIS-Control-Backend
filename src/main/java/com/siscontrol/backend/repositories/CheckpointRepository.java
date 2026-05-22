package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

    List<Checkpoint> findByInstallationIdOrderByExecutionOrderAsc(Long installationId);

    Optional<Checkpoint> findByNfcTagCode(String nfcTagCode);

    boolean existsByInstallationIdAndExecutionOrderAndStatus(Long installationId, Integer executionOrder, Integer status);

    boolean existsByInstallationIdAndExecutionOrderAndStatusAndIdNot(Long installationId, Integer executionOrder, Integer status, Long id);

    // Consulta indispensable para el cálculo dinámico del total de puntos (Número B)
    long countByInstallationIdAndStatus(Long installationId, Integer status);
}
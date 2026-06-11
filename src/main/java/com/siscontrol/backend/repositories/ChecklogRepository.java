package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChecklogRepository extends JpaRepository<Checklog, Long> {

    boolean existsByRoundExecutionIdAndCheckpointId(Long roundExecutionId, Long checkpointId);

    // === FIJANDO LA GRAMÁTICA DE JPQL DE FORMA EXPLÍCITA ===
    @Query("SELECT c FROM Checklog c WHERE c.roundExecution.id = :roundExecutionId")
    List<Checklog> findByRoundExecutionId(@Param("roundExecutionId") Long roundExecutionId);

    @Query("SELECT COUNT(c) FROM Checklog c WHERE c.roundExecution.id = :roundExecutionId")
    long countByRoundExecutionId(@Param("roundExecutionId") Long roundExecutionId);
}
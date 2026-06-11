package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoundExecutionRepository extends JpaRepository<RoundExecution, Long> {
    boolean existsByWorkerIdAndStatus(Long workerId, RoundStatus status);
    List<RoundExecution> findByWorkerId(Long workerId);
    Optional<RoundExecution> findFirstByWorkerIdAndStatusOrderByIdDesc(Long workerId, RoundStatus status);
    List<RoundExecution> findByWorkerIdAndStatusOrderByIdDesc(Long workerId, RoundStatus status);
    List<RoundExecution> findByStatus(RoundStatus status);

    // OPTIMIZACIÓN CRÍTICA: Trae la ronda junto al Guardia (worker) e Instalación en 1 sola consulta SQL
    @Query("SELECT r FROM RoundExecution r " +
            "LEFT JOIN FETCH r.worker " +
            "LEFT JOIN FETCH r.installation " +
            "WHERE r.id = :id")
    Optional<RoundExecution> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM RoundExecution r " +
            "WHERE r.worker.id = :workerId " +
            "AND r.installation.id = :installationId " +
            "AND r.startTime >= :entryTime " +
            "AND (:exitTime IS NULL OR r.startTime <= :exitTime)")
    List<RoundExecution> findRoundsForShift(
            @Param("workerId") Long workerId,
            @Param("installationId") Long installationId,
            @Param("entryTime") java.time.LocalDateTime entryTime,
            @Param("exitTime") java.time.LocalDateTime exitTime);
}
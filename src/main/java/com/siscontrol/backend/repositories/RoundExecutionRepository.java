package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoundExecutionRepository extends JpaRepository<RoundExecution, Long> {
    boolean existsByWorkerIdAndStatus(Long workerId, RoundStatus status);
    List<RoundExecution> findByWorkerId(Long workerId);
}
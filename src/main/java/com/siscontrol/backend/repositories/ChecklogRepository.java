package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChecklogRepository extends JpaRepository<Checklog, Long> {
    boolean existsByRoundExecutionIdAndCheckpointId(Long roundExecutionId, Long checkpointId);
    List<Checklog> findByRoundExecutionId(Long roundExecutionId);
    long countByRoundExecutionId(Long roundExecutionId);
}
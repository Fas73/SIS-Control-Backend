package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.RoundExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundExecutionRepository extends JpaRepository<RoundExecution, Long> {
}
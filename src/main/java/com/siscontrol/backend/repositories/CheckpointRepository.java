package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
}
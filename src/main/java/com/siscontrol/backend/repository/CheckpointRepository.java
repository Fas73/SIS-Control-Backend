package com.siscontrol.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.siscontrol.backend.entity.Checkpoint;

import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    Optional<Checkpoint> findByTagIdentifier(String tagIdentifier);

}

package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Shift;
import com.siscontrol.backend.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByWorkerId(Long userId);
    Optional<Shift> findByWorkerIdAndStatus(Long workerId, ShiftStatus status);
    Optional<Shift> findByWorkerIdAndInstallationIdAndStatus(Long workerId, Long installationId, ShiftStatus status);
}
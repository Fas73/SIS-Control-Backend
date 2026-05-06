package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Shift;
import com.siscontrol.backend.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByGuardId(Long userId);
    Optional<Shift> findByGuardIdAndStatus(Long guardId, ShiftStatus status);
    Optional<Shift> findByGuardIdAndInstallationIdAndStatus(Long guardId, Long installationId, ShiftStatus status);
}
package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.SupervisorGuard;
import com.siscontrol.backend.models.SupervisorGuardId;
import com.siscontrol.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupervisorGuardRepository extends JpaRepository<SupervisorGuard, SupervisorGuardId> {
    List<SupervisorGuard> findBySupervisor(User supervisor);
}

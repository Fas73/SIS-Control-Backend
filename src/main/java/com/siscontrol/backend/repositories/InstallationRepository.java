package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallationRepository extends JpaRepository<Installation, Long> {
}
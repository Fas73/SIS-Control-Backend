package com.siscontrol.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.siscontrol.backend.entity.Installation;

public interface InstallationRepository extends JpaRepository<Installation, Long> {

}

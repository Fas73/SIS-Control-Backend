package com.siscontrol.backend.repositories;

import com.siscontrol.backend.models.Checklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklogRepository extends JpaRepository<Checklog, Long> {
}
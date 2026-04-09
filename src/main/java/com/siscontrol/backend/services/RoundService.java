package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.repositories.ChecklogRepository;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.CheckpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoundService {

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private CheckpointRepository checkpointRepository;

    // --- Gestión de Instalaciones ---
    public Installation guardarInstalacion(Installation installation) {
        return installationRepository.save(installation);
    }

    public List<Installation> obtenerTodasLasInstalaciones() {
        return installationRepository.findAll();
    }

    // --- Gestión de Puntos de Control ---
    public Checkpoint guardarCheckpoint(Checkpoint checkpoint) {
        return checkpointRepository.save(checkpoint);
    }

    public List<Checkpoint> obtenerCheckpointsPorInstalacion(Long installationId) {
        // Esto asume que tienes el método en el repositorio o usas un filtro simple
        return checkpointRepository.findAll().stream()
                .filter(c -> c.getInstallation() != null && c.getInstallation().getId().equals(installationId))
                .toList();
    }

    @Autowired
    private ChecklogRepository checklogRepository;

    public Checklog registrarEscaneo(Checklog log) {
        // Seteamos la hora actual al momento del escaneo
        log.setTimestamp(java.time.LocalDateTime.now());
        return checklogRepository.save(log);
    }
}
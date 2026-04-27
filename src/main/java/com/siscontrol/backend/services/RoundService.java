package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoundService {

    @Autowired private InstallationRepository installationRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private ChecklogRepository checklogRepository;

    public Installation guardarInstalacion(Installation installation) {
        return installationRepository.save(installation);
    }

    public List<Installation> obtenerTodasLasInstalaciones() {
        return installationRepository.findAll();
    }

    public Checkpoint guardarCheckpoint(Checkpoint checkpoint) {
        return checkpointRepository.save(checkpoint);
    }

    // Método optimizado que devuelve DTOs
    public List<CheckpointDTO> obtenerCheckpointsPorInstalacion(Long installationId) {
        return checkpointRepository.findByInstallationId(installationId)
                .stream()
                .map(c -> new CheckpointDTO(c.getId(), c.getName(), c.getLocationDescription(), c.getNfcTagCode(), c.getInstallation().getId()))
                .collect(Collectors.toList());
    }

    public Checklog registrarEscaneo(Checklog log) {
        log.setTimestamp(java.time.LocalDateTime.now());
        return checklogRepository.save(log);
    }
}
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
    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private IncidentRepository incidentRepository;

    // --- Métodos de Gestión de Incidentes ---

    public Incident registrarIncidente(Incident incident) {
        // 1. Validar que el incidente tenga una ronda asociada
        if (incident.getRoundExecution() == null || incident.getRoundExecution().getId() == null) {
            throw new IllegalArgumentException("Error: El incidente debe estar asociado a una ejecución de ronda.");
        }

        // 2. BUSCAR LA RONDA COMPLETA en la base de datos
        RoundExecution fullRound = roundExecutionRepository.findById(incident.getRoundExecution().getId())
                .orElseThrow(() -> new IllegalArgumentException("Error: La ejecución de ronda especificada no existe."));

        // 3. Asignar la ronda completa (con su instalación y datos hidratados) al incidente
        incident.setRoundExecution(fullRound);

        // 4. Establecer la fecha de creación
        incident.setCreatedAt(java.time.LocalDateTime.now());

        // 5. Guardar
        return incidentRepository.save(incident);
    }

    public List<Incident> obtenerIncidentesPorRonda(Long roundExecutionId) {
        return incidentRepository.findByRoundExecutionId(roundExecutionId);
    }

    // --- Métodos de Gestión de Rondas ---

    public RoundExecution iniciarRonda(RoundExecution round) {
        // 1. Validar que la ronda venga con una instalación asociada
        if (round.getInstallation() == null || round.getInstallation().getId() == null) {
            throw new IllegalArgumentException("La ronda debe estar asociada a una instalación válida.");
        }

        // 2. Buscar la instalación completa en la base de datos usando el ID proporcionado
        // Esto asegura que el objeto "installation" no sea null y tenga todos sus datos
        Installation fullInstallation = installationRepository.findById(round.getInstallation().getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe una instalación con el ID: " + round.getInstallation().getId()));

        // 3. Asignamos la instalación completa, el estado y la hora de inicio
        round.setInstallation(fullInstallation);
        round.setStatus(RoundStatus.INICIADA);
        round.setStartTime(java.time.LocalDateTime.now());

        return roundExecutionRepository.save(round);
    }

    public Checklog registrarEscaneo(Checklog log) {
        // 1. Validaciones
        if (log.getRoundExecution() == null || log.getRoundExecution().getId() == null) {
            throw new IllegalArgumentException("El escaneo debe estar asociado a una ejecución de ronda.");
        }
        if (log.getCheckpoint() == null || log.getCheckpoint().getId() == null) {
            throw new IllegalArgumentException("El escaneo debe estar asociado a un checkpoint.");
        }

        // 2. Buscar la ronda completa
        RoundExecution round = roundExecutionRepository.findById(log.getRoundExecution().getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la ronda"));

        // 3. Buscar el checkpoint completo
        Checkpoint checkpoint = checkpointRepository.findById(log.getCheckpoint().getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe el checkpoint"));

        // 4. Asignar los objetos completos al log
        log.setRoundExecution(round);
        log.setCheckpoint(checkpoint);
        log.setTimestamp(java.time.LocalDateTime.now());

        return checklogRepository.save(log);
    }

    // --- Métodos de Gestión de Instalaciones ---

    public Installation guardarInstalacion(Installation installation) {
        return installationRepository.save(installation);
    }

    public List<Installation> obtenerTodasLasInstalaciones() {
        return installationRepository.findAll();
    }

    // --- Métodos de Gestión de Checkpoints ---

    public Checkpoint guardarCheckpoint(Checkpoint checkpoint) {
        // 1. Validamos que el checkpoint tenga una instalación asociada
        if (checkpoint.getInstallation() == null || checkpoint.getInstallation().getId() == null) {
            throw new IllegalArgumentException("El checkpoint debe tener una instalación asociada válida.");
        }

        // 2. Buscamos la instalación completa en la base de datos usando el ID proporcionado
        Installation fullInstallation = installationRepository.findById(checkpoint.getInstallation().getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe una instalación con el ID: " + checkpoint.getInstallation().getId()));

        // 3. Asignamos el objeto de instalación completo (con nombre, dirección, etc.) al checkpoint
        checkpoint.setInstallation(fullInstallation);

        // 4. Guardamos el checkpoint ahora que tiene la instalación completa
        return checkpointRepository.save(checkpoint);
    }

    public List<CheckpointDTO> obtenerCheckpointsPorInstalacion(Long installationId) {
        return checkpointRepository.findByInstallationId(installationId)
                .stream()
                .map(c -> new CheckpointDTO(c.getId(), c.getName(), c.getLocationDescription(), c.getNfcTagCode(), c.getInstallation().getId()))
                .collect(Collectors.toList());
    }

    public RoundExecution finalizarRonda(Long id) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la ronda con ID: " + id));

        round.setEndTime(java.time.LocalDateTime.now());

        // Usamos el Enum directamente
        round.setStatus(RoundStatus.TERMINADA);

        return roundExecutionRepository.save(round);
    }

    public RoundExecution obtenerRondaPorId(Long id) {
        return roundExecutionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la ronda con ID: " + id));
    }

    public List<RoundExecution> obtenerTodasLasRondas() {
        return roundExecutionRepository.findAll();
    }


}
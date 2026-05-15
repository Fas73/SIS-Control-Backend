package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.*;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundService {

    @Autowired private ShiftRepository shiftRepository;
    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ChecklogRepository checklogRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InstallationRepository installationRepository;

    // --- JORNADAS (ASISTENCIA) ---

    public Map<String, Object> iniciarJornada(Long userId, Long installationId) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (worker.getStatus() != 1) {
            throw new ForbiddenException("El usuario está inactivo.");
        }

        if (shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO).isPresent()) {
            throw new BadRequestException("Ya tienes una jornada en curso.");
        }

        Installation inst = installationRepository.findById(installationId)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada."));

        Shift shift = new Shift();
        shift.setWorker(worker);
        shift.setInstallation(inst);
        shift.setEntryTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.EN_CURSO);

        return Map.of("mensaje", "Jornada iniciada", "jornada", shiftRepository.save(shift));
    }

    public Map<String, Object> finalizarJornada(Long userId, Long installationId) {
        Shift shift = shiftRepository.findByWorkerIdAndInstallationIdAndStatus(userId, installationId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una jornada activa para finalizar."));

        if (roundExecutionRepository.existsByWorkerIdAndStatus(userId, RoundStatus.EN_PROGRESO)) {
            throw new BadRequestException("No puedes finalizar la jornada con una ronda en progreso.");
        }

        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);
        return Map.of("mensaje", "Jornada finalizada correctamente", "jornada", shiftRepository.save(shift));
    }

    // --- RONDAS ---

    public Map<String, Object> iniciarRonda(Long userId, Long installationId) {
        shiftRepository.findByWorkerIdAndInstallationIdAndStatus(userId, installationId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new BadRequestException("Debes iniciar jornada antes de empezar una ronda."));

        if (roundExecutionRepository.existsByWorkerIdAndStatus(userId, RoundStatus.EN_PROGRESO)) {
            throw new BadRequestException("Ya tienes una ronda en progreso.");
        }

        RoundExecution round = new RoundExecution();
        round.setWorker(userRepository.getReferenceById(userId));
        round.setInstallation(installationRepository.getReferenceById(installationId));
        round.setStartTime(LocalDateTime.now());
        round.setStatus(RoundStatus.EN_PROGRESO);

        return Map.of("mensaje", "Ronda iniciada", "ronda", roundExecutionRepository.save(round));
    }

    public Map<String, Object> finalizarRonda(Long id, String observations) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada"));

        round.setEndTime(LocalDateTime.now());
        round.setStatus(RoundStatus.FINALIZADA);
        round.setObservations(observations);

        return Map.of("mensaje", "Ronda finalizada", "ronda", roundExecutionRepository.save(round));
    }

    public Map<String, Object> registrarEscaneo(Checklog log) {
        RoundExecution round = roundExecutionRepository.findById(log.getRoundExecution().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no existe."));

        log.setScannedAt(LocalDateTime.now());
        log.setStatus(1);
        log.setCreatedBy(round.getWorker().getId());

        return Map.of("mensaje", "Escaneo registrado", "escaneo", checklogRepository.save(log));
    }

    public Map<String, Object> obtenerDetalleRonda(Long id) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada"));

        return Map.of(
                "ronda", round,
                "escaneos", checklogRepository.findByRoundExecutionId(id),
                "incidentes", incidentRepository.findByRoundExecutionId(id)
        );
    }

    public List<RoundExecution> filtrarRondas(String fecha, Long installationId, Long userId) {
        return roundExecutionRepository.findAll().stream()
                .filter(r -> {
                    boolean coincideFecha = (fecha == null) || r.getStartTime().toLocalDate().toString().equals(fecha);
                    boolean coincideInst = (installationId == null) || (r.getInstallation().getId().equals(installationId));
                    boolean coincideUser = (userId == null) || (r.getWorker().getId().equals(userId));
                    return coincideFecha && coincideInst && coincideUser;
                })
                .collect(Collectors.toList());
    }
}
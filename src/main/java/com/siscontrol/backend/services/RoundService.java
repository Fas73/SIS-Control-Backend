package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.ChecklogRepository;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.dto.NfcScanRequestDTO;
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
    @Autowired private CheckpointRepository checkpointRepository; // Agregado para validar existencia de puntos

    // --- JORNADAS ---
    public Map<String, Object> iniciarJornada(Long userId, Long installationId) {
        User guard = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (guard.getRole() != UserRole.GUARD) throw new BadRequestException("Solo guardias inician jornada.");

        if (shiftRepository.findByGuardIdAndStatus(userId, ShiftStatus.EN_CURSO).isPresent())
            throw new BadRequestException("Ya tienes una jornada en curso.");

        Installation inst = installationRepository.findById(installationId).orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada."));

        Shift shift = new Shift();
        shift.setGuard(guard);
        shift.setInstallation(inst);
        shift.setEntryTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.EN_CURSO);

        return Map.of("mensaje", "Jornada iniciada", "jornada", shiftRepository.save(shift));
    }

    public Map<String, Object> finalizarJornada(Long id) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada."));
        if (roundExecutionRepository.existsByGuardIdAndStatus(shift.getGuard().getId(), RoundStatus.EN_PROGRESO))
            throw new BadRequestException("Termina la ronda antes de cerrar jornada.");

        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);
        return Map.of("mensaje", "Jornada finalizada", "jornada", shiftRepository.save(shift));
    }

    public Map<String, Object> cancelarJornada(Long id, Long adminId) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada con ID: " + id));

        userRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado"));

        shift.setStatus(ShiftStatus.CANCELADO);
        shift.setExitTime(LocalDateTime.now());

        return Map.of("mensaje", "Jornada cancelada por administrador", "jornada", shiftRepository.save(shift));
    }

    // --- RONDAS ---
    public Map<String, Object> iniciarRonda(Long userId, Long installationId) {
        shiftRepository.findByGuardIdAndInstallationIdAndStatus(userId, installationId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new BadRequestException("No tienes jornada activa aquí."));

        if (roundExecutionRepository.existsByGuardIdAndStatus(userId, RoundStatus.EN_PROGRESO))
            throw new BadRequestException("Ya hay una ronda en progreso.");

        RoundExecution round = new RoundExecution();
        round.setGuard(userRepository.getReferenceById(userId));
        round.setInstallation(installationRepository.getReferenceById(installationId));
        round.setStartTime(LocalDateTime.now());
        round.setStatus(RoundStatus.EN_PROGRESO);

        return Map.of("mensaje", "Ronda iniciada", "ronda", roundExecutionRepository.save(round));
    }

    public Map<String, Object> finalizarRonda(Long id) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + id));

        if (round.getStatus() != RoundStatus.EN_PROGRESO) {
            throw new BadRequestException("La ronda ya no está en progreso.");
        }

        round.setEndTime(LocalDateTime.now());
        round.setStatus(RoundStatus.FINALIZADA);

        return Map.of("mensaje", "Ronda finalizada", "ronda", roundExecutionRepository.save(round));
    }

    public Map<String, Object> cancelarRonda(Long id, Long adminId, String motivo) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + id));

        userRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado"));

        round.setStatus(RoundStatus.CANCELADA);
        round.setEndTime(LocalDateTime.now());

        return Map.of(
                "mensaje", "Ronda cancelada por administrador",
                "motivo", motivo,
                "ronda", roundExecutionRepository.save(round)
        );
    }

    // --- REGISTRO DE ESCANEO (Corregido con validación de existencia) ---
    public Map<String, Object> registrarEscaneo(Checklog log) {
        // 1. Validar Ronda
        RoundExecution round = roundExecutionRepository.findById(log.getRoundExecution().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada."));

        if (round.getStatus() != RoundStatus.EN_PROGRESO) throw new BadRequestException("Ronda no activa.");

        // 2. Validar Existencia del Checkpoint (Para evitar error de Constraint en DB)
        if (log.getCheckpoint() == null || log.getCheckpoint().getId() == null) {
            throw new BadRequestException("El punto de control es obligatorio.");
        }

        if (!checkpointRepository.existsById(log.getCheckpoint().getId())) {
            throw new ResourceNotFoundException("El punto de control con ID " + log.getCheckpoint().getId() + " no existe.");
        }

        // 3. Validar duplicados
        if (checklogRepository.existsByRoundExecutionIdAndCheckpointId(round.getId(), log.getCheckpoint().getId()))
            throw new BadRequestException("Punto ya escaneado.");

        log.setTimestamp(LocalDateTime.now());
        return Map.of("mensaje", "Escaneo registrado", "escaneo", checklogRepository.save(log));
    }

    //metodo registro en nfc, similar al anterior pero con validacion de ronda y punto de control, ademas de validar que el tag nfc exista y corresponda a un punto de control registrado en la instalacion de la ronda
    public Checklog registrarEscaneoNfc(NfcScanRequestDTO request) {
        RoundExecution execution = roundExecutionRepository.findById(request.getExecutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada."));

        if (execution.getStatus() != RoundStatus.EN_PROGRESO) {
            throw new BadRequestException("Ronda no está en progreso.");
        }

        Checkpoint checkpoint = checkpointRepository.findByNfcTagCode(request.getNfcTagCode())
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint NFC no encontrado."));

        if (checklogRepository.existsByRoundExecutionIdAndCheckpointId(execution.getId(), checkpoint.getId())) {
            throw new BadRequestException("Punto de control ya escaneado en esta ronda.");
        }
        
        Checklog log = new Checklog();
        log.setRoundExecution(execution);
        log.setCheckpoint(checkpoint);
        log.setTimestamp(LocalDateTime.now());

        return checklogRepository.save(log);
    }

    // --- CONSULTAS (Sin cambios, manteniendo tu lógica original) ---
    public Map<String, Object> obtenerEstadisticasGlobales(LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRondas", roundExecutionRepository.count());
        stats.put("totalIncidentes", incidentRepository.count());
        stats.put("jornadasActivas", shiftRepository.findAll().stream().filter(s -> s.getStatus() == ShiftStatus.EN_CURSO).count());
        return stats;
    }

    public Map<String, Object> obtenerDetalleRonda(Long id) {
        RoundExecution round = roundExecutionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada"));
        return Map.of("ronda", round, "escaneos", checklogRepository.findByRoundExecutionId(id), "incidentes", incidentRepository.findByRoundExecutionId(id));
    }

    public List<RoundExecution> filtrarRondas(String fecha, Long installationId, Long userId) {
        List<RoundExecution> todas = roundExecutionRepository.findAll();

        return todas.stream()
                .filter(r -> {
                    boolean coincideFecha = (fecha == null) || r.getStartTime().toLocalDate().toString().equals(fecha);
                    boolean coincideInst = (installationId == null) || (r.getInstallation().getId().equals(installationId));
                    boolean coincideUser = (userId == null) || (r.getGuard().getId().equals(userId));

                    return coincideFecha && coincideInst && coincideUser;
                })
                .collect(Collectors.toList());
    }

    public Object obtenerRondasSegunRol(Long requesterId) {
        User user = userRepository.findById(requesterId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return (user.getRole() == UserRole.GUARD) ? roundExecutionRepository.findByGuardId(requesterId) : roundExecutionRepository.findAll();
    }
}
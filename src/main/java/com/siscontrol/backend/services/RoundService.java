package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.*;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Map<String, Object> iniciarJornada(Long userId, Long installationId) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (worker.getStatus() != 1) {
            throw new ForbiddenException("El usuario está inactivo. Contacte al administrador.");
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

        return Map.of("mensaje", "Jornada inició exitosamente", "jornada", shiftRepository.save(shift));
    }

    @Transactional
    public Map<String, Object> finalizarJornada(Long userId, Long installationId) {
        Shift shift = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes ninguna jornada en curso para esta instalación."));

        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);

        return Map.of("mensaje", "Salida registrada con éxito", "jornada", shiftRepository.save(shift));
    }

    @Transactional
    public Map<String, Object> cancelarJornadaAdministrativamente(Long id, Long adminId) {
        validarAdminOSupervisor(adminId);

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada (Shift) no encontrada con ID: " + id));

        if (shift.getStatus() == ShiftStatus.FINALIZADO) {
            throw new BadRequestException("Esta jornada ya se encuentra finalizada.");
        }

        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);

        return Map.of(
                "mensaje", "Jornada cancelada/finalizada de forma remota por el administrador",
                "jornada", shiftRepository.save(shift)
        );
    }

    // --- CONTROL DE RONDAS ---

    @Transactional
    public Map<String, Object> iniciarRonda(Long userId, Long installationId) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (worker.getStatus() != 1) {
            throw new ForbiddenException("El guardia se encuentra inactivo. No puede iniciar rondas.");
        }

        // Validación extra de negocio: Asegurar que el guardia tenga asistencia iniciada
        boolean tieneJornadaActiva = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO).isPresent();
        if (!tieneJornadaActiva) {
            throw new BadRequestException("Debe iniciar su jornada laboral (Asistencia) antes de comenzar una ronda.");
        }

        Installation inst = installationRepository.findById(installationId)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada"));

        RoundExecution round = new RoundExecution();
        round.setWorker(worker);
        round.setInstallation(inst);
        round.setStartTime(LocalDateTime.now());
        round.setStatus(RoundStatus.EN_PROGRESO);

        return Map.of("mensaje", "Ronda iniciada", "ronda", roundExecutionRepository.save(round));
    }

    @Transactional
    public Map<String, Object> finalizarRonda(Long id, String observations) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada"));

        if (round.getStatus() == RoundStatus.FINALIZADA) {
            throw new BadRequestException("Esta ronda ya fue finalizada anteriormente.");
        }

        round.setStatus(RoundStatus.FINALIZADA);
        round.setEndTime(LocalDateTime.now());
        round.setObservations(observations);

        return Map.of("mensaje", "Ronda finalizada con éxito", "ronda", roundExecutionRepository.save(round));
    }

    @Transactional
    public Map<String, Object> cancelarRondaAdministrativamente(Long id, Long adminId, String motivo) {
        validarAdminOSupervisor(adminId);

        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + id));

        if (round.getStatus() == RoundStatus.FINALIZADA) {
            throw new BadRequestException("No se puede cancelar una ronda que ya fue finalizada.");
        }

        round.setStatus(RoundStatus.FINALIZADA);
        round.setEndTime(LocalDateTime.now());
        round.setObservations("[CANCELACIÓN ADMINISTRATIVA]: " + (motivo != null ? motivo : "Sin motivo especificado"));

        return Map.of(
                "mensaje", "Ronda cancelada administrativamente con éxito",
                "ronda", roundExecutionRepository.save(round)
        );
    }

    // --- ESCANEOS ---

    @Transactional
    public Map<String, Object> registrarEscaneo(Checklog log) {
        if (log.getRoundExecution() == null || log.getRoundExecution().getId() == null) {
            throw new BadRequestException("El escaneo debe estar vinculado a un ID de ejecución de ronda válido.");
        }

        RoundExecution round = roundExecutionRepository.findById(log.getRoundExecution().getId())
                .orElseThrow(() -> new ResourceNotFoundException("La ronda vinculada al escaneo no existe."));

        // Protección: Evitar escaneos en rondas ya cerradas
        if (round.getStatus() == RoundStatus.FINALIZADA) {
            throw new BadRequestException("No se pueden registrar puntos de control en una ronda que ya está finalizada.");
        }

        log.setScannedAt(LocalDateTime.now());
        log.setStatus(1); // Marcaje activo/válido

        return Map.of("mensaje", "Escaneo de punto de control registrado exitosamente", "escaneo", checklogRepository.save(log));
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
        // Optimización menor: Si tu base de datos crece, filtrar en memoria con .stream().filter() puede ponerse lento.
        // En el futuro considera migrar esto a un query dinámico en el repositorio usando Specification o @Query.
        return roundExecutionRepository.findAll().stream()
                .filter(r -> {
                    boolean coincideFecha = (fecha == null) || r.getStartTime().toLocalDate().toString().equals(fecha);
                    boolean coincideInst = (installationId == null) || (r.getInstallation().getId().equals(installationId));
                    boolean coincideUser = (userId == null) || (r.getWorker().getId().equals(userId));
                    return coincideFecha && coincideInst && coincideUser;
                })
                .collect(Collectors.toList());
    }

    private void validarAdminOSupervisor(Long editorId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario supervisor/administrador no encontrado"));
        if (editor.getRole() == UserRole.GUARD) {
            throw new ForbiddenException("Acceso denegado: Los guardias no tienen los privilegios requeridos para esta acción administrativa.");
        }
    }
}
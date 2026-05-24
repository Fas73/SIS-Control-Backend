package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.*;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled; // <-- IMPORTANTE
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

    @Autowired private AlertService alertService;

    // --- JORNADAS (ASISTENCIA) ---

    @Transactional
    public Map<String, Object> iniciarJornada(Long userId, Long installationId) {
        return iniciarJornada(userId, installationId, null);
    }

    @Transactional
    public Map<String, Object> iniciarJornada(Long userId, Long installationId, LocalDateTime deviceEntryTime) {
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
        shift.setDeviceEntryTime(deviceEntryTime);
        shift.setStatus(ShiftStatus.EN_CURSO);

        return Map.of("mensaje", "Jornada inició exitosamente", "jornada", shiftRepository.save(shift));
    }

    @Transactional
    public Map<String, Object> finalizarJornada(Long userId, Long installationId) {
        return finalizarJornada(userId, installationId, null);
    }

    @Transactional
    public Map<String, Object> finalizarJornada(Long userId, Long installationId, LocalDateTime deviceExitTime) {
        Shift shift = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes ninguna jornada en curso para esta instalación."));

        shift.setExitTime(LocalDateTime.now());
        shift.setDeviceExitTime(deviceExitTime);
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
        return iniciarRonda(userId, installationId, null);
    }

    @Transactional
    public Map<String, Object> iniciarRonda(Long userId, Long installationId, LocalDateTime deviceStartTime) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (worker.getStatus() != 1) {
            throw new ForbiddenException("El guardia se encuentra inactivo. No puede iniciar rondas.");
        }

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
        round.setDeviceStartTime(deviceStartTime);
        round.setStatus(RoundStatus.EN_PROGRESO);

        return Map.of("mensaje", "Ronda iniciada", "ronda", roundExecutionRepository.save(round));
    }

    @Transactional
    public Map<String, Object> finalizarRonda(Long id, String observations) {
        return finalizarRonda(id, observations, null);
    }

    @Transactional
    public Map<String, Object> finalizarRonda(Long id, String observations, LocalDateTime deviceEndTime) {
        RoundExecution round = roundExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada"));

        if (round.getStatus() == RoundStatus.FINALIZADA) {
            throw new BadRequestException("Esta ronda ya fue finalizada anteriormente.");
        }

        round.setStatus(RoundStatus.FINALIZADA);
        round.setEndTime(LocalDateTime.now());
        round.setDeviceEndTime(deviceEndTime);
        round.setObservations(observations);

        RoundExecution guardada = roundExecutionRepository.save(round);

        // DISPARAR ALERTA INFORMATIVA AUTOMÁTICA (Pestaña Info)
        Incident infoRonda = new Incident();
        infoRonda.setTitle("ℹ️ Ronda completada");
        infoRonda.setDescription("Todos los checkpoints verificados por " + round.getWorker().getFullName() + " en " + round.getInstallation().getName());
        infoRonda.setSeverity("Baja"); // Pestaña de información (Azul)
        infoRonda.setStatus(1); // Auto-atendido / Cerrado
        infoRonda.setType(com.siscontrol.backend.enums.IncidentType.HALLAZGO);
        infoRonda.setRoundExecution(guardada);

        alertService.registrarYDispararAlerta(infoRonda);

        return Map.of("mensaje", "Ronda finalizada con éxito", "ronda", guardada);
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
                "mensaje", "Ronda canceled administrativamente con éxito",
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

        if (round.getStatus() == RoundStatus.FINALIZADA) {
            throw new BadRequestException("No se pueden registrar puntos de control en una ronda que ya está finalizada.");
        }

        log.setScannedAt(LocalDateTime.now());

        if (log.getStatus() == null) {
            log.setStatus(1);
        }

        Checklog guardado = checklogRepository.save(log);

        if (guardado.getStatus() == 2) {
            Incident alertaOmision = new Incident();
            alertaOmision.setTitle("⚠️ Checkpoint no escaneado");

            String motivo = (guardado.getNotes() != null && !guardado.getNotes().trim().isEmpty())
                    ? guardado.getNotes()
                    : "Ronda incompleta detectada (Punto saltado sin observaciones)";

            alertaOmision.setDescription(motivo);
            alertaOmision.setSeverity("Media"); // Pestaña de Advertencia
            alertaOmision.setStatus(0);
            alertaOmision.setType(com.siscontrol.backend.enums.IncidentType.MANTENCION);
            alertaOmision.setRoundExecution(round);
            alertaOmision.setChecklog(guardado);

            alertService.registrarYDispararAlerta(alertaOmision);
        }

        String mensajeExito = (guardado.getStatus() == 2)
                ? "Punto de control omitido con justificación correctamente. Secuencia liberada."
                : "Escaneo de punto de control registrado exitosamente.";

        return Map.of("mensaje", mensajeExito, "escaneo", guardado);
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

    // --- ESTADO EN TIEMPO REAL ---
    public Map<String, Object> verificarEstadoActual(Long userId) {
        Optional<Shift> jornadaOpt = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO);

        Optional<RoundExecution> rondaOpt = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getWorker() != null && r.getWorker().getId().equals(userId) && r.getStatus() == RoundStatus.EN_PROGRESO)
                .findFirst();

        Map<String, Object> estado = new HashMap<>();
        estado.put("jornadaActiva", jornadaOpt.isPresent());
        estado.put("rondaActiva", rondaOpt.isPresent());
        estado.put("jornada", jornadaOpt.orElse(null));
        estado.put("ronda", rondaOpt.orElse(null));

        return estado;
    }

    // --- AUTOMATIZACIÓN: DETECCIÓN DE INACTIVIDAD (Pestaña Advertencia) ---
    @Scheduled(fixedRate = 300000) // Se ejecuta automáticamente cada 5 minutos
    @Transactional
    public void verificarRondasInactivas() {
        LocalDateTime limiteInactividad = LocalDateTime.now().minusMinutes(15);

        List<RoundExecution> rondasActivas = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO)
                .toList();

        for (RoundExecution ronda : rondasActivas) {
            List<Checklog> escaneos = checklogRepository.findByRoundExecutionId(ronda.getId());

            LocalDateTime ultimaActividad = ronda.getStartTime();
            if (!escaneos.isEmpty()) {
                escaneos.sort((c1, c2) -> c2.getScannedAt().compareTo(c1.getScannedAt()));
                ultimaActividad = escaneos.get(0).getScannedAt();
            }

            if (ultimaActividad.isBefore(limiteInactividad)) {
                List<Incident> incidentesExistentes = incidentRepository.findByRoundExecutionId(ronda.getId());
                boolean yaAlertado = incidentesExistentes.stream()
                        .anyMatch(i -> i.getTitle().equals("⏳ Alerta: Ronda Incompleta"));

                if (!yaAlertado) {
                    Incident advertencia = new Incident();
                    advertencia.setTitle("⏳ Alerta: Ronda Incompleta");
                    advertencia.setDescription("Se ha detectado inactividad prolongada en " + ronda.getInstallation().getName() + " (más de 15 minutos sin registrar lecturas NFC).");
                    advertencia.setSeverity("Media"); // Pestaña de Advertencia (Amarillo)
                    advertencia.setStatus(0);
                    advertencia.setType(com.siscontrol.backend.enums.IncidentType.MANTENCION);
                    advertencia.setRoundExecution(ronda);

                    alertService.registrarYDispararAlerta(advertencia);
                }
            }
        }
    }

    private void validarAdminOSupervisor(Long editorId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario supervisor/administrador no encontrado"));
        if (editor.getRole() == UserRole.GUARD) {
            throw new ForbiddenException("Acceso denegado: Los guardias no tienen los privilegios requeridos para esta acción administrativa.");
        }
    }
}
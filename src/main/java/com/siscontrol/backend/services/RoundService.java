package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.*;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.dto.RoundHistoryItemDTO;
import com.siscontrol.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled; // <-- IMPORTANTE
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundService {

    @Autowired private ShiftRepository shiftRepository;
    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ChecklogRepository checklogRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InstallationRepository installationRepository;
    @Autowired private AlertService alertService;

    @Autowired private AlertService alertService;

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
    public Map<String, Object> finalizarJornada(Long userId, Long installationId, Double latitude, Double longitude) {
        Shift shift = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes ninguna jornada en curso para esta instalación."));

        Optional<RoundExecution> rondaActivaOpt = roundExecutionRepository.findFirstByWorkerIdAndStatusOrderByIdDesc(userId, RoundStatus.EN_PROGRESO);

        if (rondaActivaOpt.isPresent()) {
            RoundExecution rondaActiva = rondaActivaOpt.get();
            rondaActiva.setStatus(RoundStatus.FINALIZADA);
            rondaActiva.setEndTime(LocalDateTime.now());
            rondaActiva.setObservations("[CIERRE AUTOMÁTICO]: Finalizada automáticamente debido a que el guardia registró su salida (Check-out).");
            roundExecutionRepository.save(rondaActiva);
        }

        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);
        shift.setLatitude(latitude);
        shift.setLongitude(longitude);
        Shift savedShift = shiftRepository.save(shift);

        Incident checkoutIncident = new Incident();
        checkoutIncident.setTitle("JORNADA FINALIZADA");
        checkoutIncident.setDescription("El guardia ha registrado su salida (Check-out).");
        checkoutIncident.setSeverity("Baja");
        checkoutIncident.setStatus(1);
        checkoutIncident.setType(com.siscontrol.backend.enums.IncidentType.JORNADA);
        checkoutIncident.setShiftId(savedShift.getId());
        checkoutIncident.setClientTimestamp(LocalDateTime.now());
        checkoutIncident.setLatitude(latitude);
        checkoutIncident.setLongitude(longitude);
        alertService.registrarYDispararAlerta(checkoutIncident);

        return Map.of("mensaje", "Salida registrada con éxito", "jornada", savedShift);
    }

    @Transactional
    public Map<String, Object> cancelarJornadaAdministrativamente(Long id, Long adminId) {
        validarAdminOSupervisor(adminId);

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada (Shift) no encontrada con ID: " + id));

        if (shift.getStatus() == ShiftStatus.FINALIZADO) {
            throw new BadRequestException("Esta jornada ya se encuentra finalizada.");
        }

        User worker = null;
        try {
            worker = shift.getWorker();
            if (worker != null) {
                worker.getId(); // Trigger resolution
            }
        } catch (Exception e) {
            worker = null;
            shift.setWorker(null); // Desenlazar proxy huérfano para evitar Error 500 en Jackson
        }

        final Long workerId = worker != null ? worker.getId() : null;
        String workerName = worker != null ? worker.getFullName() : "Desconocido";
        String observacionesCierre = "[CIERRE AUTOMÁTICO]: Tu jornada laboral ha sido finalizada remotamente por la administración.";

        // 1. Si el guardia estaba en una ronda, la cerramos limpiamente en la Base de Datos
        if (workerId != null) {
            Optional<RoundExecution> rondaActivaOpt = roundExecutionRepository.findFirstByWorkerIdAndStatusOrderByIdDesc(workerId, RoundStatus.EN_PROGRESO);

            if (rondaActivaOpt.isPresent()) {
                RoundExecution rondaActiva = rondaActivaOpt.get();
                rondaActiva.setStatus(RoundStatus.FINALIZADA);
                rondaActiva.setEndTime(LocalDateTime.now());
                rondaActiva.setObservations(observacionesCierre);
                roundExecutionRepository.save(rondaActiva);
            }
        }

        // 2. 🔥 NOTIFICACIÓN FULMINANTE: Se ejecuta SIEMPRE (con o sin ronda activa)
        alertService.dispararNotificacionDirecta(
                "Jornada Finalizada por Administración",
                observacionesCierre,
                workerName,
                workerId,
                shift.getId()
        );

        // 3. Finalizamos el Shift
        shift.setExitTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.FINALIZADO);
        Shift savedShift = shiftRepository.save(shift);

        Incident checkoutIncident = new Incident();
        checkoutIncident.setTitle("JORNADA FINALIZADA");
        checkoutIncident.setDescription(observacionesCierre);
        checkoutIncident.setSeverity("Alta");
        checkoutIncident.setStatus(1);
        checkoutIncident.setType(com.siscontrol.backend.enums.IncidentType.JORNADA);
        checkoutIncident.setShiftId(savedShift.getId());
        checkoutIncident.setClientTimestamp(LocalDateTime.now());
        alertService.registrarYDispararAlerta(checkoutIncident);

        return Map.of(
                "mensaje", "Jornada y rondas asociadas finalizadas remotamente",
                "jornada", savedShift
        );
    }

    // --- CONTROL DE RONDAS ---

    @Transactional
    public Map<String, Object> iniciarRonda(Long userId, Long installationId) {
        return iniciarRonda(userId, installationId, null, null);
    }

    @Transactional
    public Map<String, Object> iniciarRonda(Long userId, Long installationId, Double latitude, Double longitude) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (worker.getStatus() != 1) {
            throw new ForbiddenException("El guardia se encuentra inactivo. No puede iniciar rondas.");
        }

        Shift shift = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO)
                .orElseThrow(() -> new BadRequestException("Debe iniciar su jornada laboral (Asistencia) antes de comenzar una ronda."));

        if (latitude != null && longitude != null) {
            shift.setLatitude(latitude);
            shift.setLongitude(longitude);
            shiftRepository.save(shift);
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

        RoundExecution guardada = roundExecutionRepository.save(round);

        // DISPARAR ALERTA INFORMATIVA AUTOMÁTICA (Pestaña Info)
        Incident infoRonda = new Incident();
        infoRonda.setTitle("ℹ️ Ronda completada");
        infoRonda.setDescription("Todos los checkpoints verificados por " + round.getWorker().getFullName() + " en " + round.getInstallation().getName());
        infoRonda.setSeverity("Baja"); // Pestaña de información (Azul)
        infoRonda.setStatus(1); // Auto-atendido / Cerrado
        infoRonda.setType(com.siscontrol.backend.enums.IncidentType.RONDA);
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

        String observacionesFinales = "[CANCELACIÓN ADMINISTRATIVA]: " + (motivo != null ? motivo : "Sin motivo especificado");

        round.setStatus(RoundStatus.FINALIZADA);
        round.setEndTime(LocalDateTime.now());
        round.setObservations(observacionesFinales);

        RoundExecution guardada = roundExecutionRepository.save(round);

        // 🔥 MODIFICACIÓN CRÍTICA EN TIEMPO REAL:
        // Forzamos el envío inmediato al celular del guardia usando tu canal de WebSockets existente
        Incident pushAdmin = new Incident();
        pushAdmin.setTitle("Ronda Cancelada administrativamente");
        pushAdmin.setDescription(observacionesFinales);
        pushAdmin.setSeverity("Alta");
        pushAdmin.setStatus(1); // Marcado como gestionado/visto para que no ensucie el dashboard web
        pushAdmin.setType(com.siscontrol.backend.enums.IncidentType.OTRO);
        pushAdmin.setRoundExecution(guardada);

        alertService.registrarYDispararAlerta(pushAdmin);

        return Map.of(
                "mensaje", "Ronda canceled administrativamente con éxito",
                "ronda", guardada
        );
    }

    // --- ESCANEOS ---

    @Transactional
    public Map<String, Object> registrarEscaneo(Checklog log) {
        // === SINTONIZADOR DE PAYLOAD UNIVERSAL (BLINDAJE DE CONTEXTO HIBERNATE) ===

        Long rId = null;
        Long cId = null;

        // 1. Capturar los IDs sin importar si vienen planos o anidados en el JSON
        if (log.getRoundExecution() != null && log.getRoundExecution().getId() != null) {
            rId = log.getRoundExecution().getId();
        } else if (log.getRoundExecutionId() != null) {
            rId = log.getRoundExecutionId();
        }

        if (log.getCheckpoint() != null && log.getCheckpoint().getId() != null) {
            cId = log.getCheckpoint().getId();
        } else if (log.getCheckpointId() != null) {
            cId = log.getCheckpointId();
        }

        // 2. LIMPIEZA CRÍTICA: Vaciamos las referencias para que Hibernate reinicie el buffer
        log.setRoundExecution(null);
        log.setCheckpoint(null);

        // 3. Forzar a Spring Data a buscar instancias frescas y activas en la BD
        if (rId != null) {
            RoundExecution reReal = roundExecutionRepository.findById(rId).orElse(null);
            log.setRoundExecution(reReal);
        }

        if (cId != null) {
            Checkpoint cpReal = checkpointRepository.findById(cId).orElse(null);
            log.setCheckpoint(cpReal);
        }
        // ===========================================================================

        // DE AQUÍ HACIA ABAJO TU LÓGICA DE NEGOCIO ORIGINAL SE MANTIENE 100% INTACTA
        if (log.getRoundExecution() == null || log.getRoundExecution().getId() == null) {
            throw new BadRequestException("El escaneo debe estar vinculado a un ID de ejecución de ronda válido.");
        }

        RoundExecution round = roundExecutionRepository.findById(log.getRoundExecution().getId())
                .orElseThrow(() -> new ResourceNotFoundException("La ronda vinculada al escaneo no existe."));

        // AJUSTE PARA SOPORTE OFFLINE
        if (log.getScannedAt() == null) {
            log.setScannedAt(LocalDateTime.now());
        } else {
            if (log.getScannedAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
                throw new BadRequestException("La estampa de tiempo provista no es válida (reloj alterado en dispositivo).");
            }
        }

        if (round.getStatus() == RoundStatus.FINALIZADA) {
            // Manejo de concurrencia y resiliencia offline:
            // Si el escaneo ocurrió antes o durante el tiempo activo de la ronda (antes o igual a round.getEndTime()), se permite registrar.
            if (round.getEndTime() != null && log.getScannedAt().isAfter(round.getEndTime())) {
                throw new BadRequestException("No se pueden registrar puntos de control en una ronda que ya está finalizada.");
            }
        }

        if (log.getStatus() == null) {
            log.setStatus(1);
        }

        // 1. Guardar el checklog de forma limpia en la tabla 'checklogs'
        Checklog guardado = checklogRepository.save(log);

        // Actualizar coordenadas de la Jornada activa si se envían en el checklog
        if (guardado.getLatitude() != null && guardado.getLongitude() != null && round.getWorker() != null) {
            Optional<Shift> shiftOpt = shiftRepository.findByWorkerIdAndStatus(round.getWorker().getId(), ShiftStatus.EN_CURSO);
            if (shiftOpt.isPresent()) {
                Shift shift = shiftOpt.get();
                shift.setLatitude(guardado.getLatitude());
                shift.setLongitude(guardado.getLongitude());
                shiftRepository.save(shift);
            }
        }

        // 2. AUTOMATIZACIÓN DE OMISIONES: Si el estatus es 2, creamos la alerta en 'incidents'
        if (guardado.getStatus() == 2) {
            Incident contingencia = new Incident();
            contingencia.setTitle("ALERTA: Checkpoint no escaneado");
            contingencia.setDescription("El punto '" + (guardado.getCheckpoint() != null ? guardado.getCheckpoint().getName() : "Entrada Principal")
                    + "' fue omitido. Justificación: " + (guardado.getNotes() != null ? guardado.getNotes() : "Sin notas"));
            contingencia.setSeverity("Media");
            contingencia.setStatus(0); // 0 = Pendiente en el dashboard web
            contingencia.setType(com.siscontrol.backend.enums.IncidentType.MANTENCION);
            contingencia.setRoundExecution(guardado.getRoundExecution());
            contingencia.setChecklog(guardado); // Enlaza el incidente al checklog para el celular
            contingencia.setImageUrl(guardado.getImageUrl()); // Copia el link de la foto de Firebase

            // Disparar WebSocket para que la pantalla de la administración se entere en vivo
            alertService.registrarYDispararAlerta(contingencia);
        }

        // 3. Retornar la respuesta exacta que tu Frontend móvil espera recibir
        String mensajeExito = (guardado.getStatus() == 2)
                ? "Punto de control omitido con justificación correctamente. Secuencia liberada."
                : "Escaneo de punto de control registrado exitosamente.";

        return Map.of("mensaje", mensajeExito, "escaneo", guardado);
    }

    // --- REQUERIMIENTO: OBTENER DETALLE DE RONDA CON INCIDENTES MAPEADOS ---
    public Map<String, Object> obtenerDetalleRonda(Long id, Long requesterId) {
        // CAMBIO DE RENDIMIENTO: Ahora invoca la consulta optimizada con JOIN FETCH
        RoundExecution round = roundExecutionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + id));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario solicitante no encontrado"));

        // Conserva tu validación de seguridad intacta
        if (requester.getRole() == com.siscontrol.backend.enums.UserRole.GUARD) {
            if (!round.getWorker().getId().equals(requesterId)) {
                throw new ForbiddenException("Acceso denegado: No posees privilegios para auditar rondas de otros operarios.");
            }
        }

        // Inyectamos IncidentService para mapear los incidentes a DTOs con sus checkpoints y órdenes resueltos
        List<com.siscontrol.backend.dto.IncidentDTO> incidentesMapeados = incidentRepository.findByRoundExecutionId(id).stream()
                .map(incident -> {
                    com.siscontrol.backend.dto.IncidentDTO dto = new com.siscontrol.backend.dto.IncidentDTO();
                    dto.setId(incident.getId());
                    dto.setTitle(incident.getTitle());
                    dto.setDescription(incident.getDescription());
                    dto.setSeverity(incident.getSeverity());
                    dto.setImageUrl(incident.getImageUrl());
                    dto.setType(incident.getType() != null ? incident.getType().name() : null);
                    dto.setCreatedAt(incident.getCreatedAt());
                    dto.setStatus(incident.getStatus());
                    dto.setRoundExecutionId(id);

                    if (incident.getRoundExecution() != null && incident.getRoundExecution().getWorker() != null) {
                        dto.setUsername(incident.getRoundExecution().getWorker().getFullName());
                    }

// El cruce crítico que pide el Frontend para la tarjeta del celular
                    if (incident.getChecklog() != null) {
                        dto.setChecklogId(incident.getChecklog().getId());
                        dto.setChecklogImageUrl(incident.getChecklog().getImageUrl()); // <-- AGREGA ESTA LÍNEA AQUÍ

                        if (incident.getChecklog().getCheckpoint() != null) {
                            dto.setCheckpointName(incident.getChecklog().getCheckpoint().getName());
                            dto.setCheckpointOrder(incident.getChecklog().getCheckpoint().getExecutionOrder());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // Mantiene exactamente tus claves originales del Map ("ronda", "escaneos", "incidentes")
        return Map.of(
                "ronda", round,
                "escaneos", checklogRepository.findByRoundExecutionId(id),
                "incidentes", incidentesMapeados // <-- Ahora viaja la estructura enriquecida que espera Android
        );
    }

    // --- REQUERIMIENTO SINCRONIZADO CON TOLERANCIA DE ZONA HORARIA CHILE ---
    public List<RoundHistoryItemDTO> filtrarRondas(String fecha, Long installationId, Long userId) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        List<RoundExecution> todasLasRondas;
        if (userId != null) {
            todasLasRondas = roundExecutionRepository.findByWorkerId(userId).stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> installationId == null || (r.getInstallation() != null && r.getInstallation().getId().equals(installationId)))
                .filter(r -> {
                    if (fecha == null || fecha.trim().isEmpty()) return true;
                    String fechaRondaStr = r.getStartTime().toLocalDate().toString();
                    if (fecha.contains("-") && fecha.indexOf("-") == 2) {
                        String[] parts = fecha.split("-");
                        String fechaConvertida = parts[2] + "-" + parts[1] + "-" + parts[0];
                        return fechaRondaStr.equals(fechaConvertida);
                    }
                    return fechaRondaStr.equals(fecha) || r.getStartTime().toString().contains(fecha);
                })
                .collect(Collectors.toList());
        } else {
            todasLasRondas = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> installationId == null || (r.getInstallation() != null && r.getInstallation().getId().equals(installationId)))
                .filter(r -> {
                    if (fecha == null || fecha.trim().isEmpty()) return true;
                    String fechaRondaStr = r.getStartTime().toLocalDate().toString();
                    if (fecha.contains("-") && fecha.indexOf("-") == 2) {
                        String[] parts = fecha.split("-");
                        String fechaConvertida = parts[2] + "-" + parts[1] + "-" + parts[0];
                        return fechaRondaStr.equals(fechaConvertida);
                    }
                    return fechaRondaStr.equals(fecha) || r.getStartTime().toString().contains(fecha);
                })
                .collect(Collectors.toList());
        }

        Map<Long, List<RoundExecution>> agrupadasPorId = todasLasRondas.stream()
                .collect(Collectors.groupingBy(RoundExecution::getId));

        List<RoundHistoryItemDTO> listaConsolidada = new ArrayList<>();

        for (Map.Entry<Long, List<RoundExecution>> entrada : agrupadasPorId.entrySet()) {
            RoundExecution r = entrada.getValue().get(0);
            List<Checklog> logs = checklogRepository.findByRoundExecutionId(r.getId());

            int executed = (int) logs.stream().filter(l -> l.getStatus() == 1 || l.getStatus() == 2).count();
            int totalCheckpoints = (r.getInstallation() != null) 
                ? (int) checkpointRepository.countByInstallationIdAndStatus(r.getInstallation().getId(), 1) 
                : 0;

            long incidentesRealesCount = incidentRepository.findByRoundExecutionId(r.getId()).stream()
                    .filter(i -> i.getSeverity() != null && !i.getSeverity().equalsIgnoreCase("Baja"))
                    .count();

            long duration = 0;
            if (r.getStartTime() != null && r.getEndTime() != null) {
                duration = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
            }

            RoundHistoryItemDTO dto = new RoundHistoryItemDTO();
            dto.setId(r.getId());
            dto.setInstallationName(r.getInstallation() != null ? r.getInstallation().getName() : "Desconocida");
            dto.setStartTime(r.getStartTime() != null ? r.getStartTime().format(dtf) : "---");
            dto.setEndTime(r.getEndTime() != null ? r.getEndTime().format(dtf) : "En progreso");
            dto.setDurationMinutes(duration);

            if (executed >= totalCheckpoints && totalCheckpoints > 0) {
                dto.setStatus("FINALIZADA");
                dto.setStatusDisplay("Completada");
            } else {
                dto.setStatus(r.getStatus() != null ? r.getStatus().name() : "DESCONOCIDO");
                dto.setStatusDisplay("Incompleta");
            }

            dto.setCheckpointsExecuted(executed);
            dto.setCheckpointsTotal(totalCheckpoints);
            dto.setIncidentCount((int) incidentesRealesCount);
            dto.setDetailedSummary(r.getObservations() != null ? r.getObservations() : "Sin observaciones.");

            listaConsolidada.add(dto);
        }

        listaConsolidada.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));
        return listaConsolidada;
    }

    // --- ESTADO EN TIEMPO REAL ---
    public Map<String, Object> verificarEstadoActual(Long userId) {
        Optional<Shift> jornadaOpt = shiftRepository.findByWorkerIdAndStatus(userId, ShiftStatus.EN_CURSO);

        // 1. Intentamos buscar la ronda que está actualmente activa (Flujo normal en progreso)
        Optional<RoundExecution> rondaOpt = roundExecutionRepository.findFirstByWorkerIdAndStatusOrderByIdDesc(userId, RoundStatus.EN_PROGRESO);

        boolean esRondaActivaReal = rondaOpt.isPresent();

        // 2. SOPORTE DE MENSAJES ADMINISTRATIVOS: Si no hay activa, buscamos si le acaban de cerrar una administrativamente
        if (rondaOpt.isEmpty()) {
            rondaOpt = roundExecutionRepository.findByWorkerIdAndStatusOrderByIdDesc(userId, RoundStatus.FINALIZADA).stream()
                    .filter(r -> r.getObservations() != null
                            && (r.getObservations().contains("[CANCELACIÓN ADMINISTRATIVA]") || r.getObservations().contains("[CIERRE AUTOMÁTICO]"))
                            && r.getEndTime() != null
                            && r.getEndTime().isAfter(LocalDateTime.now().minusHours(1))) // Ventana de tolerancia de 1 hora
                    .findFirst(); // Tomamos la última cerrada ya que vienen ordenadas por Id DESC
        }

        Map<String, Object> estado = new HashMap<>();
        estado.put("jornadaActiva", jornadaOpt.isPresent());
        // 'rondaActiva' le dice a Android si debe pintar la pantalla de escaneo o no.
        // Enviamos true solo si está EN_PROGRESO para no romper la navegación nativa de la App.
        estado.put("rondaActiva", esRondaActivaReal);
        estado.put("jornada", jornadaOpt.orElse(null));
        estado.put("ronda", rondaOpt.orElse(null)); // <-- Aquí viaja la ronda FINALIZADA con sus observaciones durante 1 hora

        return estado;
    }

    // --- AUTOMATIZACIÓN: DETECCIÓN DE INACTIVIDAD ---
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void verificarRondasInactivas() {
        LocalDateTime limiteInactividad = LocalDateTime.now().minusMinutes(15);

        List<RoundExecution> rondasActivas = roundExecutionRepository.findByStatus(RoundStatus.EN_PROGRESO);

        for (RoundExecution ronda : rondasActivas) {
            List<Checklog> escaneos = checklogRepository.findByRoundExecutionId(ronda.getId());

            LocalDateTime ultimaActivity = ronda.getStartTime();
            if (!escaneos.isEmpty()) {
                escaneos.sort((c1, c2) -> c2.getScannedAt().compareTo(c1.getScannedAt()));
                ultimaActivity = escaneos.get(0).getScannedAt();
            }

            if (ultimaActivity.isBefore(limiteInactividad)) {
                List<Incident> incidentesExistentes = incidentRepository.findByRoundExecutionId(ronda.getId());
                boolean yaAlertado = incidentesExistentes.stream()
                        .anyMatch(i -> i.getTitle().equals("Alerta: Ronda Incompleta"));

                if (!yaAlertado) {
                    Incident advertencia = new Incident();
                    advertencia.setTitle("Alerta: Ronda Incompleta");
                    advertencia.setDescription("Se ha detectado inactividad prolongada en " + ronda.getInstallation().getName() + " (más de 15 minutos sin registrar lecturas NFC).");
                    advertencia.setSeverity("Media");
                    advertencia.setStatus(0);
                    advertencia.setType(com.siscontrol.backend.enums.IncidentType.MANTENCION);
                    advertencia.setRoundExecution(ronda);

                    alertService.registrarYDispararAlerta(advertencia);
                }
            }
        }
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
package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.RoundStatus;
import com.siscontrol.backend.enums.ShiftStatus;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.dto.RoundHistoryItemDTO;
import com.siscontrol.backend.dto.AdminDashboardDTO;
import com.siscontrol.backend.dto.DashboardActiveRoundDTO;
import com.siscontrol.backend.dto.DashboardActiveShiftDTO;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private ChecklogRepository checklogRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private InstallationRepository installationRepository;
    @Autowired private SupervisorGuardRepository supervisorGuardRepository;

    // 1. Metodo de estadísticas globales (Dashboard General)
    public Map<String, Object> obtenerEstadisticasGlobales(LocalDateTime inicio, LocalDateTime fin) {
        List<RoundExecution> todas = roundExecutionRepository.findAll();

        List<RoundExecution> filtradas = todas.stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> (inicio == null || !r.getStartTime().isBefore(inicio)) &&
                        (fin == null || !r.getStartTime().isAfter(fin)))
                .collect(Collectors.toList());

        long completadas = filtradas.stream().filter(r -> r.getStatus() == RoundStatus.FINALIZADA).count();
        long enProgreso = filtradas.stream().filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO).count();

        return Map.of(
                "totalRondas", filtradas.size(),
                "completadas", completadas,
                "enProgreso", enProgreso,
                "periodo", Map.of("desde", inicio != null ? inicio : "inicio", "hasta", fin != null ? fin : "ahora")
        );
    }

    public Object obtenerHistorialJornadas(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Shift> jornadas = (requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.SUPERVISOR)
                ? shiftRepository.findAll()
                : shiftRepository.findAll();

        return validarRespuesta(jornadas, "No se encontraron registros de jornadas.");
    }

    public Object obtenerHistorialRondas(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<RoundExecution> rondas = (requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.SUPERVISOR)
                ? roundExecutionRepository.findAll()
                : roundExecutionRepository.findAll();

        return validarRespuesta(rondas, "No se encontraron registros de rondas.");
    }

    public Map<String, Object> obtenerEstadisticasGuardias() {
        LocalDateTime hoyInicio = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime hoyFin = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        long totalGuardias = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.GUARD)
                .count();

        long guardiasActivos = shiftRepository.findAll().stream()
                .filter(s -> s.getEntryTime() != null &&
                        !s.getEntryTime().isBefore(hoyInicio) &&
                        !s.getEntryTime().isAfter(hoyFin) &&
                        s.getExitTime() == null)
                .count();

        return Map.of(
                "totalGuardias", totalGuardias,
                "guardiasConJornadaIniciada", guardiasActivos
        );
    }

    public Map<String, Object> obtenerHistorialRondasGuardia(Long guardId, LocalDateTime inicio, LocalDateTime fin) {
        if (!userRepository.existsById(guardId)) {
            throw new ResourceNotFoundException("Guardia no encontrado.");
        }

        List<RoundExecution> todasMisRondas = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getWorker() != null && r.getWorker().getId().equals(guardId))
                .filter(r -> r.getStartTime() != null)
                .filter(r -> {
                    // Tolerancia de Husos Horarios para la App Móvil en Chile
                    if (inicio != null && r.getStartTime().isBefore(inicio.minusHours(4))) return false;
                    if (fin != null && r.getStartTime().isAfter(fin.plusHours(4))) return false;
                    return true;
                })
                .collect(Collectors.toList());

        Map<Long, List<RoundExecution>> agrupadas = todasMisRondas.stream()
                .collect(Collectors.groupingBy(RoundExecution::getId));

        List<RoundHistoryItemDTO> dtoList = new ArrayList<>();
        List<Shift> todasMisJornadas = shiftRepository.findAll();

        for (Map.Entry<Long, List<RoundExecution>> entrada : agrupadas.entrySet()) {
            RoundExecution r = entrada.getValue().get(0);

            int totalPoints = 0;
            if (r.getInstallation() != null) {
                totalPoints = (int) checkpointRepository.countByInstallationIdAndStatus(r.getInstallation().getId(), 1);
            }

            int scannedPoints = (int) checklogRepository.countByRoundExecutionId(r.getId());

            long incidentesRealesCount = incidentRepository.findByRoundExecutionId(r.getId()).stream()
                    .filter(i -> i.getSeverity() != null && !i.getSeverity().equalsIgnoreCase("Baja"))
                    .count();

            String statusDisplay = "Incompleta";
            if (r.getStatus() == RoundStatus.EN_PROGRESO) {
                statusDisplay = "En curso";
            } else if (r.getStatus() == RoundStatus.FINALIZADA || (totalPoints > 0 && scannedPoints >= totalPoints)) {
                statusDisplay = "Completada";
            }

            String textoIncidente = (incidentesRealesCount == 1) ? "1 incidente reportado" : incidentesRealesCount + " incidentes";
            if (incidentesRealesCount == 0) textoIncidente = "Sin incidentes";
            String detailedSummary = scannedPoints + "/" + totalPoints + " puntos marcados. " + textoIncidente;

            String shiftStart = null;
            String shiftEnd = null;
            java.time.LocalDate fechaRonda = r.getStartTime().toLocalDate();

            // Buscamos la última jornada del día para evitar quedar amarrados al primer turno de la madrugada
            Optional<Shift> shiftDelDia = todasMisJornadas.stream()
                    .filter(s -> s.getWorker() != null && s.getWorker().getId().equals(guardId))
                    .filter(s -> s.getEntryTime() != null && s.getEntryTime().toLocalDate().equals(fechaRonda))
                    .max(Comparator.comparing(Shift::getEntryTime));

            if (shiftDelDia.isPresent()) {
                shiftStart = shiftDelDia.get().getEntryTime().toString();
                shiftEnd = shiftDelDia.get().getExitTime() != null ? shiftDelDia.get().getExitTime().toString() : "En curso";
            }

            dtoList.add(new RoundHistoryItemDTO(
                    r.getId(),
                    (r.getInstallation() != null) ? r.getInstallation().getName() : "Instalación Desconocida",
                    r.getStartTime().toString(),
                    r.getEndTime() != null ? r.getEndTime().toString() : null,
                    r.getEndTime() != null ? java.time.Duration.between(r.getStartTime(), r.getEndTime()).toMinutes() : 0,
                    r.getStatus().name(),
                    statusDisplay,
                    scannedPoints,
                    totalPoints,
                    (int) incidentesRealesCount,
                    shiftStart,
                    shiftEnd,
                    detailedSummary
            ));
        }

        dtoList.sort((d1, d2) -> Long.compare(d2.getId(), d1.getId()));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", dtoList.size());
        respuesta.put("completas", dtoList.stream().filter(d -> d.getStatusDisplay().equalsIgnoreCase("Completada")).count());
        respuesta.put("porcentajeExito", dtoList.isEmpty() ? "0%" : (int)((dtoList.stream().filter(d -> d.getStatusDisplay().equalsIgnoreCase("Completada")).count() * 100) / dtoList.size()) + "%");
        respuesta.put("rondas", dtoList);

        return respuesta;
    }

    public AdminDashboardDTO obtenerDashboardAdmin() {
        LocalDateTime inicioHoy = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MIN);
        LocalDateTime finHoy = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MAX);

        List<User> todosLosUsuarios = userRepository.findAll();
        int totalGuards = (int) todosLosUsuarios.stream()
                .filter(u -> u.getRole() == UserRole.GUARD && u.getStatus() == 1)
                .count();

        List<Shift> todasLasJornadas = shiftRepository.findAll();
        int activeShiftsCount = (int) todasLasJornadas.stream()
                .filter(s -> s.getStatus() == ShiftStatus.EN_CURSO)
                .count();

        List<RoundExecution> todasLasRondas = roundExecutionRepository.findAll();
        List<RoundExecution> rondasDeHoy = todasLasRondas.stream()
                .filter(r -> r.getStartTime() != null && !r.getStartTime().isBefore(inicioHoy) && !r.getStartTime().isAfter(finHoy))
                .toList();

        int totalRoundsToday = rondasDeHoy.size();
        int roundsInProgress = (int) rondasDeHoy.stream().filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO).count();
        int roundsCompleted = (int) rondasDeHoy.stream().filter(r -> r.getStatus() == RoundStatus.FINALIZADA).count();

        List<Incident> todosLosIncidentes = incidentRepository.findAll();
        int totalIncidents = todosLosIncidentes.size();
        int pendingIncidents = (int) todosLosIncidentes.stream().filter(i -> i.getStatus() == 0).count();

        List<Installation> todasLasInstalaciones = installationRepository.findAll();
        int totalInstallations = todasLasInstalaciones.size();
        int activeInstallationsCount = (int) todasLasInstalaciones.stream().filter(i -> i.getStatus() == 1).count();

        List<DashboardActiveRoundDTO> activeRoundsList = todasLasRondas.stream()
                .filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO)
                .map(r -> {
                    String guardName = r.getWorker() != null ? r.getWorker().getFullName() : "Desconocido";
                    String location = r.getInstallation() != null ? r.getInstallation().getName() : "Ubicación desconocida";
                    float progreso = 0.5f;

                    return new DashboardActiveRoundDTO(
                            r.getId(),
                            guardName,
                            location,
                            progreso,
                            "En progreso",
                            r.getStatus().name()
                    );
                }).toList();

        List<DashboardActiveShiftDTO> activeShiftsList = todasLasJornadas.stream()
                .filter(s -> s.getStatus() == ShiftStatus.EN_CURSO)
                .map(s -> {
                    String guardName = s.getWorker() != null ? s.getWorker().getFullName() : "Desconocido";
                    String location = s.getInstallation() != null ? s.getInstallation().getName() : "Sin ubicación";
                    String entryTime = s.getEntryTime() != null ? s.getEntryTime().toString() : "N/A";

                    Double lat = s.getLatitude();
                    Double lon = s.getLongitude();

                    if ((lat == null || lat == 0.0 || lon == null || lon == 0.0) && s.getInstallation() != null) {
                        lat = s.getInstallation().getLatitude();
                        lon = s.getInstallation().getLongitude();
                    }

                    return new DashboardActiveShiftDTO(
                            s.getId(),
                            guardName,
                            location,
                            entryTime,
                            lat,
                            lon
                    );
                }).toList();

        return new AdminDashboardDTO(
                totalGuards, activeShiftsCount, totalRoundsToday, roundsInProgress, roundsCompleted,
                totalIncidents, pendingIncidents, totalInstallations, activeInstallationsCount,
                activeRoundsList, activeShiftsList
        );
    }

    public AdminDashboardDTO obtenerDashboardSupervisor(Long supervisorId) {
        LocalDateTime inicioHoy = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MIN);
        LocalDateTime finHoy = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MAX);

        // 1. Obtener los IDs de los guardias asignados a este supervisor
        List<Long> assignedGuardIds = supervisorGuardRepository.findBySupervisorId(supervisorId)
                .stream()
                .map(sg -> sg.getGuard().getId())
                .collect(Collectors.toList());

        int totalGuards = assignedGuardIds.size();

        // Si no tiene guardias, devolvemos un dashboard vacío pero con instalaciones globales si es necesario
        // En este caso, mostraremos 0 para todo excepto las instalaciones en las que podrían estar trabajando
        
        List<Shift> todasLasJornadas = shiftRepository.findAll().stream()
                .filter(s -> s.getWorker() != null && assignedGuardIds.contains(s.getWorker().getId()))
                .toList();
                
        int activeShiftsCount = (int) todasLasJornadas.stream()
                .filter(s -> s.getStatus() == ShiftStatus.EN_CURSO)
                .count();

        List<RoundExecution> todasLasRondas = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getWorker() != null && assignedGuardIds.contains(r.getWorker().getId()))
                .toList();
                
        List<RoundExecution> rondasDeHoy = todasLasRondas.stream()
                .filter(r -> r.getStartTime() != null && !r.getStartTime().isBefore(inicioHoy) && !r.getStartTime().isAfter(finHoy))
                .toList();

        int totalRoundsToday = rondasDeHoy.size();
        int roundsInProgress = (int) rondasDeHoy.stream().filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO).count();
        int roundsCompleted = (int) rondasDeHoy.stream().filter(r -> r.getStatus() == RoundStatus.FINALIZADA).count();

        // Filtramos incidentes que ocurran en los shifts o rondas de estos guardias
        List<Long> shiftIds = todasLasJornadas.stream().map(Shift::getId).toList();
        List<Long> roundIds = todasLasRondas.stream().map(RoundExecution::getId).toList();
        
        List<Incident> todosLosIncidentes = incidentRepository.findAll().stream()
                .filter(i -> (i.getShiftId() != null && shiftIds.contains(i.getShiftId())) || 
                             (i.getRoundExecution() != null && roundIds.contains(i.getRoundExecution().getId())))
                .toList();
                
        int totalIncidents = todosLosIncidentes.size();
        int pendingIncidents = (int) todosLosIncidentes.stream().filter(i -> i.getStatus() == 0).count();

        // Opcional: mostrar solo instalaciones donde haya jornadas activas de sus guardias
        List<Long> activeInstallationIds = todasLasJornadas.stream()
                .filter(s -> s.getStatus() == ShiftStatus.EN_CURSO && s.getInstallation() != null)
                .map(s -> s.getInstallation().getId())
                .distinct()
                .toList();

        int activeInstallationsCount = activeInstallationIds.size();
        // Dejamos el total de instalaciones como las activas para no confundir con las globales
        int totalInstallations = activeInstallationsCount; 

        List<DashboardActiveRoundDTO> activeRoundsList = todasLasRondas.stream()
                .filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO)
                .map(r -> {
                    String guardName = r.getWorker() != null ? r.getWorker().getFullName() : "Desconocido";
                    String location = r.getInstallation() != null ? r.getInstallation().getName() : "Ubicación desconocida";
                    float progreso = 0.5f;

                    return new DashboardActiveRoundDTO(
                            r.getId(),
                            guardName,
                            location,
                            progreso,
                            "En progreso",
                            r.getStatus().name()
                    );
                }).toList();

        List<DashboardActiveShiftDTO> activeShiftsList = todasLasJornadas.stream()
                .filter(s -> s.getStatus() == ShiftStatus.EN_CURSO)
                .map(s -> {
                    String guardName = s.getWorker() != null ? s.getWorker().getFullName() : "Desconocido";
                    String location = s.getInstallation() != null ? s.getInstallation().getName() : "Sin ubicación";
                    String entryTime = s.getEntryTime() != null ? s.getEntryTime().toString() : "N/A";

                    Double lat = s.getLatitude();
                    Double lon = s.getLongitude();

                    if ((lat == null || lat == 0.0 || lon == null || lon == 0.0) && s.getInstallation() != null) {
                        lat = s.getInstallation().getLatitude();
                        lon = s.getInstallation().getLongitude();
                    }

                    return new DashboardActiveShiftDTO(
                            s.getId(),
                            guardName,
                            location,
                            entryTime,
                            lat,
                            lon
                    );
                }).toList();

        return new AdminDashboardDTO(
                totalGuards, activeShiftsCount, totalRoundsToday, roundsInProgress, roundsCompleted,
                totalIncidents, pendingIncidents, totalInstallations, activeInstallationsCount,
                activeRoundsList, activeShiftsList
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.siscontrol.backend.dto.ShiftReportDTO obtenerReporteJornada(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada (Shift) no encontrada con ID: " + shiftId));

        User worker = shift.getWorker();
        Installation inst = shift.getInstallation();

        String workerName = worker != null ? worker.getFullName() : "Desconocido";
        String installationName = inst != null ? inst.getName() : "Desconocida";

        // Obtener rondas asociadas en la ventana del turno
        List<RoundExecution> rounds = roundExecutionRepository.findRoundsForShift(
                worker != null ? worker.getId() : null,
                inst != null ? inst.getId() : null,
                shift.getEntryTime(),
                shift.getExitTime()
        );

        // Contar checkpoints activos de la instalación
        int totalCheckpointsOfInstallation = 0;
        if (inst != null) {
            totalCheckpointsOfInstallation = (int) checkpointRepository.countByInstallationIdAndStatus(inst.getId(), 1);
        }

        // Calcular total planificado: 1 ronda por hora de turno (mínimo 1)
        int totalRoundsPlanned = 12; // default
        if (shift.getEntryTime() != null) {
            LocalDateTime end = shift.getExitTime() != null ? shift.getExitTime() : LocalDateTime.now();
            long hours = Duration.between(shift.getEntryTime(), end).toHours();
            totalRoundsPlanned = hours > 0 ? (int) hours : 1;
        }

        int totalRoundsExecuted = rounds.size();

        // Armar detalles de las rondas
        List<com.siscontrol.backend.dto.ShiftReportDTO.RoundDetailDTO> rondasDTO = new ArrayList<>();
        int totalPuntosEscaneados = 0;
        int totalPuntosOmitidos = 0;

        for (RoundExecution r : rounds) {
            List<Checklog> checklogs = checklogRepository.findByRoundExecutionId(r.getId());
            List<Incident> incidents = incidentRepository.findByRoundExecutionId(r.getId());

            int escaneados = 0;
            int omitidos = 0;
            List<com.siscontrol.backend.dto.ShiftReportDTO.ChecklogDetailDTO> checklogsDTO = new ArrayList<>();
            for (Checklog log : checklogs) {
                if (log.getStatus() != null) {
                    if (log.getStatus() == 1) {
                        escaneados++;
                        totalPuntosEscaneados++;
                    } else if (log.getStatus() == 2) {
                        omitidos++;
                        totalPuntosOmitidos++;
                    }
                }
                String checkpointName = log.getCheckpoint() != null ? log.getCheckpoint().getName() : "Desconocido";
                checklogsDTO.add(new com.siscontrol.backend.dto.ShiftReportDTO.ChecklogDetailDTO(
                        checkpointName,
                        log.getStatus(),
                        log.getScannedAt(),
                        log.getImageUrl()
                ));
            }

            List<com.siscontrol.backend.dto.ShiftReportDTO.IncidentDetailDTO> incidentesDTO = new ArrayList<>();
            for (Incident inc : incidents) {
                incidentesDTO.add(new com.siscontrol.backend.dto.ShiftReportDTO.IncidentDetailDTO(
                        inc.getId(),
                        inc.getTitle(),
                        inc.getDescription(),
                        inc.getSeverity(),
                        inc.getImageUrl(),
                        inc.getCreatedAt(),
                        inc.getStatus()
                ));
            }

            // Calcular estado de la ronda
            String statusStr = "INCOMPLETA";
            if (r.getStatus() == RoundStatus.EN_PROGRESO) {
                statusStr = "EN_PROGRESO";
            } else if (totalCheckpointsOfInstallation > 0 && (escaneados + omitidos) >= totalCheckpointsOfInstallation) {
                statusStr = "COMPLETADA";
            }

            rondasDTO.add(new com.siscontrol.backend.dto.ShiftReportDTO.RoundDetailDTO(
                    r.getId(),
                    r.getStartTime(),
                    r.getEndTime(),
                    r.getObservations(),
                    statusStr,
                    checklogsDTO,
                    incidentesDTO
            ));
        }

        // Buscar todos los incidentes asociados al shift o a las rondas del shift para el reporte de nivel superior
        List<Incident> shiftIncidentsList = new ArrayList<>();
        List<Incident> byShiftId = incidentRepository.findByShiftId(shiftId);
        shiftIncidentsList.addAll(byShiftId);

        List<Long> roundIds = rounds.stream().map(RoundExecution::getId).toList();
        List<Incident> byRounds = incidentRepository.findByRoundExecutionIdIn(roundIds).stream()
                .filter(i -> !shiftIncidentsList.contains(i))
                .toList();
        shiftIncidentsList.addAll(byRounds);

        List<com.siscontrol.backend.dto.ShiftReportDTO.IncidentDetailDTO> shiftIncidentesDTO = new ArrayList<>();
        for (Incident inc : shiftIncidentsList) {
            shiftIncidentesDTO.add(new com.siscontrol.backend.dto.ShiftReportDTO.IncidentDetailDTO(
                    inc.getId(),
                    inc.getTitle(),
                    inc.getDescription(),
                    inc.getSeverity(),
                    inc.getImageUrl(),
                    inc.getCreatedAt(),
                    inc.getStatus()
            ));
        }

        int totalAlertasGeneradas = shiftIncidentsList.size();
        int puntosTotales = totalRoundsExecuted * totalCheckpointsOfInstallation;

        com.siscontrol.backend.dto.ShiftReportDTO.MetricsDTO metricas = new com.siscontrol.backend.dto.ShiftReportDTO.MetricsDTO(
                puntosTotales,
                totalPuntosEscaneados,
                totalPuntosOmitidos,
                totalAlertasGeneradas
        );

        return new com.siscontrol.backend.dto.ShiftReportDTO(
                shift.getId(),
                workerName,
                installationName,
                shift.getEntryTime(),
                shift.getExitTime(),
                totalRoundsPlanned,
                totalRoundsExecuted,
                rondasDTO,
                shiftIncidentesDTO,
                metricas
        );
    }

    private List<?> validarRespuesta(List<?> lista, String mensaje) {
        if (lista == null || lista.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return lista;
    }
}
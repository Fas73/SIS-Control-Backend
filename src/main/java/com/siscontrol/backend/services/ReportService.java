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

                    return new DashboardActiveShiftDTO(
                            s.getId(),
                            guardName,
                            location,
                            entryTime
                    );
                }).toList();

        return new AdminDashboardDTO(
                totalGuards, activeShiftsCount, totalRoundsToday, roundsInProgress, roundsCompleted,
                totalIncidents, pendingIncidents, totalInstallations, activeInstallationsCount,
                activeRoundsList, activeShiftsList
        );
    }

    private List<?> validarRespuesta(List<?> lista, String mensaje) {
        if (lista == null || lista.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return lista;
    }
}
package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CsvReportResponseDTO;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.repositories.ChecklogRepository;
import com.siscontrol.backend.repositories.IncidentRepository;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvReportService {

    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ChecklogRepository checklogRepository;
    @Autowired private IncidentRepository incidentRepository;

    private static final String REPORT_DIR = "reportes";

    public CsvReportResponseDTO generarReporteRondasCsv() {
        File directory = new File(REPORT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String timestampStr = now.format(formatter);
        String fileName = "reporte_rondas_" + timestampStr + ".csv";
        String filePath = REPORT_DIR + File.separator + fileName;

        int rowCount = 0;

        try (FileOutputStream fos = new FileOutputStream(filePath);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // Agregar BOM UTF-8 para Excel en Windows
            writer.write('\ufeff');

            // Encabezados
            writer.write("ID Instalación,Nombre Instalación,ID Guardia,Nombre Guardia,ID Ronda,Estado Ronda,Inicio Ronda,Término Ronda,ID Checkpoint,Nombre Checkpoint,Fecha/Hora Marcación,Minutos desde anterior,Alertas/Incidentes,Observación final,Generado en,Descripción Original,Tipo Incidente IA,Prioridad IA,Resumen IA,Acción Sugerida IA,Requiere Atención Inmediata\n");

            List<RoundExecution> rondas = roundExecutionRepository.findAll();
            String generatedAtStr = now.toString();

            for (RoundExecution ronda : rondas) {
                String instId = ronda.getInstallation() != null ? String.valueOf(ronda.getInstallation().getId()) : "";
                String instName = ronda.getInstallation() != null ? ronda.getInstallation().getName() : "";
                String workerId = ronda.getWorker() != null ? String.valueOf(ronda.getWorker().getId()) : "";
                String workerName = ronda.getWorker() != null ? ronda.getWorker().getFullName() : "";
                String roundId = String.valueOf(ronda.getId());
                String status = ronda.getStatus() != null ? ronda.getStatus().name() : "";
                String start = ronda.getStartTime() != null ? ronda.getStartTime().toString() : "";
                String end = ronda.getEndTime() != null ? ronda.getEndTime().toString() : "";
                String obs = ronda.getObservations() != null ? ronda.getObservations() : "";

                List<Checklog> checklogs = checklogRepository.findByRoundExecutionIdOrderByScannedAtAsc(ronda.getId());
                List<Incident> roundIncidents = incidentRepository.findByRoundExecutionId(ronda.getId());

                if (checklogs.isEmpty()) {
                    writer.write(String.join(",",
                            escapeCsv(instId), escapeCsv(instName), escapeCsv(workerId), escapeCsv(workerName),
                            escapeCsv(roundId), escapeCsv(status), escapeCsv(start), escapeCsv(end),
                            "", "", "", "", escapeCsv(getIncidentesResumen(roundIncidents, null)),
                            escapeCsv(obs), escapeCsv(generatedAtStr),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "descripcionOriginal")),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "tipoIncidenteIA")),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "prioridadIA")),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "resumenIA")),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "accionSugeridaIA")),
                            escapeCsv(getIncidentesCampoIA(roundIncidents, null, "requiereAtencionInmediata"))
                    ) + "\n");
                    rowCount++;
                } else {
                    LocalDateTime previousTime = null;

                    // Si la ronda tiene incidentes generales (sin checklog asociado), se reportan en una fila propia al inicio
                    boolean tieneIncidentesGenerales = roundIncidents.stream().anyMatch(i -> i.getChecklog() == null);
                    if (tieneIncidentesGenerales) {
                        writer.write(String.join(",",
                                escapeCsv(instId), escapeCsv(instName), escapeCsv(workerId), escapeCsv(workerName),
                                escapeCsv(roundId), escapeCsv(status), escapeCsv(start), escapeCsv(end),
                                "", "", "", "", escapeCsv(getIncidentesResumen(roundIncidents, null)),
                                escapeCsv(obs), escapeCsv(generatedAtStr),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "descripcionOriginal")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "tipoIncidenteIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "prioridadIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "resumenIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "accionSugeridaIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, null, "requiereAtencionInmediata"))
                        ) + "\n");
                        rowCount++;
                    }

                    for (Checklog log : checklogs) {
                        String checkpointId = log.getCheckpoint() != null ? String.valueOf(log.getCheckpoint().getId()) : "";
                        String checkpointName = log.getCheckpoint() != null ? log.getCheckpoint().getName() : "";
                        String scannedAt = log.getScannedAt() != null ? log.getScannedAt().toString() : "";
                        
                        String minsSince = "";
                        if (previousTime != null && log.getScannedAt() != null) {
                            minsSince = String.valueOf(Duration.between(previousTime, log.getScannedAt()).toMinutes());
                        } else if (previousTime == null && log.getScannedAt() != null) {
                            minsSince = "0"; // Criterio usado: Para el primer checkpoint, el valor es 0
                        }

                        if (log.getScannedAt() != null) {
                            previousTime = log.getScannedAt();
                        }

                        writer.write(String.join(",",
                                escapeCsv(instId), escapeCsv(instName), escapeCsv(workerId), escapeCsv(workerName),
                                escapeCsv(roundId), escapeCsv(status), escapeCsv(start), escapeCsv(end),
                                escapeCsv(checkpointId), escapeCsv(checkpointName), escapeCsv(scannedAt), escapeCsv(minsSince),
                                escapeCsv(getIncidentesResumen(roundIncidents, log.getId())),
                                escapeCsv(obs), escapeCsv(generatedAtStr),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "descripcionOriginal")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "tipoIncidenteIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "prioridadIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "resumenIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "accionSugeridaIA")),
                                escapeCsv(getIncidentesCampoIA(roundIncidents, log.getId(), "requiereAtencionInmediata"))
                        ) + "\n");
                        rowCount++;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar el reporte CSV: " + e.getMessage());
        }

        // Reemplazar barra invertida si existe para asegurar URL válida
        String downloadUrl = "/api/reportes/csv/descargar/" + fileName;

        return new CsvReportResponseDTO(fileName, filePath, downloadUrl, now, rowCount);
    }

    private String getIncidentesResumen(List<Incident> roundIncidents, Long checklogId) {
        if (roundIncidents == null || roundIncidents.isEmpty()) return "";
        
        List<Incident> filtered;
        if (checklogId != null) {
             filtered = roundIncidents.stream()
                .filter(i -> i.getChecklog() != null && i.getChecklog().getId().equals(checklogId))
                .collect(Collectors.toList());
        } else {
             filtered = roundIncidents.stream()
                .filter(i -> i.getChecklog() == null)
                .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) return "";

        return filtered.stream()
                .map(i -> i.getTitle() + " - " + i.getDescription())
                .collect(Collectors.joining("; "));
    }

    private String getIncidentesCampoIA(List<Incident> roundIncidents, Long checklogId, String campo) {
        if (roundIncidents == null || roundIncidents.isEmpty()) return "";
        
        List<Incident> filtered;
        if (checklogId != null) {
             filtered = roundIncidents.stream()
                .filter(i -> i.getChecklog() != null && i.getChecklog().getId().equals(checklogId))
                .collect(Collectors.toList());
        } else {
             filtered = roundIncidents.stream()
                .filter(i -> i.getChecklog() == null)
                .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) return "";

        return filtered.stream()
                .map(i -> {
                    switch (campo) {
                        case "descripcionOriginal":
                            return i.getDescripcionOriginal() != null && !i.getDescripcionOriginal().trim().isEmpty()
                                    ? i.getDescripcionOriginal()
                                    : (i.getDescription() != null ? i.getDescription() : "");
                        case "tipoIncidenteIA":
                            return i.getTipoIncidenteIA() != null ? i.getTipoIncidenteIA() : "";
                        case "prioridadIA":
                            return i.getPrioridadIA() != null ? i.getPrioridadIA() : "";
                        case "resumenIA":
                            return i.getResumenIA() != null ? i.getResumenIA() : "";
                        case "accionSugeridaIA":
                            return i.getAccionSugeridaIA() != null ? i.getAccionSugeridaIA() : "";
                        case "requiereAtencionInmediata":
                            return i.getRequiereAtencionInmediata() != null ? i.getRequiereAtencionInmediata().toString() : "false";
                        default:
                            return "";
                    }
                })
                .collect(Collectors.joining("; "));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        value = value.replace("\"", "\"\"");
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}

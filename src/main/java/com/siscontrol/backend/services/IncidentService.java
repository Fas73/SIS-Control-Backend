package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.repositories.IncidentRepository;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.repositories.ChecklogRepository;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.enums.IncidentType;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private RoundExecutionRepository roundExecutionRepository;

    @Autowired
    private ChecklogRepository checklogRepository;

    @Autowired
    private GeminiIncidentAnalyzerService geminiIncidentAnalyzerService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IncidentService.class);

    @Transactional
    public IncidentDTO reportarIncidente(IncidentDTO dto) {
        IncidentType tipoFinal;
        try {
            tipoFinal = IncidentType.valueOf(dto.getType().toUpperCase().trim());
        } catch (Exception e) {
            String opciones = java.util.Arrays.toString(IncidentType.values());
            throw new IllegalArgumentException("Tipo de incidente inválido. Use uno de estos: " + opciones);
        }

        RoundExecution round = roundExecutionRepository.findById(dto.getRoundExecutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + dto.getRoundExecutionId()));

        Checklog log = null;
        if (dto.getChecklogId() != null) {
            log = checklogRepository.findById(dto.getChecklogId()).orElse(null);
        }

        Incident entity = new Incident();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setDescripcionOriginal(dto.getDescription()); // Guardar siempre la descripción original sin modificar
        entity.setSeverity(dto.getSeverity());
        entity.setImageUrl(dto.getImageUrl());
        entity.setType(tipoFinal);
        entity.setRoundExecution(round);
        entity.setChecklog(log);

        Incident saved = incidentRepository.save(entity);
        return convertirADTO(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> obtenerTodos() {
        return incidentRepository.findAllOptimized().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentDTO obtenerPorId(Long id) {
        Incident incidente = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente no encontrado con ID: " + id));
        return convertirADTO(incidente);
    }

    // Expone un punto de acceso público del mapper para controladores externos
    public IncidentDTO forzarMapeoDirecto(Incident i) {
        return this.convertirADTO(i);
    }

    // --- METODO DE MAPEÓ CORREGIDO CON CRUCE DE TEXTO COMPLETO ---
    private IncidentDTO convertirADTO(Incident i) {
        IncidentDTO dto = new IncidentDTO();
        dto.setId(i.getId());
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setSeverity(i.getSeverity());
        dto.setImageUrl(i.getImageUrl());
        dto.setType(i.getType() != null ? i.getType().name() : null);
        dto.setCreatedAt(i.getCreatedAt());
        dto.setStatus(i.getStatus());

        // --- MAPEO DE CAMPOS IA GEMINI ---
        dto.setDescripcionOriginal(i.getDescripcionOriginal());
        dto.setTipoIncidenteIA(i.getTipoIncidenteIA());
        dto.setPrioridadIA(i.getPrioridadIA());
        dto.setResumenIA(i.getResumenIA());
        dto.setAccionSugeridaIA(i.getAccionSugeridaIA());
        dto.setRequiereAtencionInmediata(i.getRequiereAtencionInmediata());
        dto.setEstadoAnalisisIA(i.getEstadoAnalisisIA());
        dto.setFechaAnalisisIA(i.getFechaAnalisisIA());
        dto.setModeloIA(i.getModeloIA());

        // Desglose relacional para evitar nulos y mapear nombres reales a Android
        if (i.getRoundExecution() != null) {
            dto.setRoundExecutionId(i.getRoundExecution().getId());

            if (i.getRoundExecution().getWorker() != null) {
                dto.setUsername(i.getRoundExecution().getWorker().getFullName());
            }
            if (i.getRoundExecution().getInstallation() != null) {
                dto.setClientName(i.getRoundExecution().getInstallation().getClientName());
            }
        }

        if (i.getChecklog() != null) {
            dto.setChecklogId(i.getChecklog().getId());
            if (i.getChecklog().getCheckpoint() != null) {
                dto.setCheckpointName(i.getChecklog().getCheckpoint().getName());
            }
        }

        return dto;
    }

    @Transactional
    public IncidentDTO analizarConIA(Long id) {
        Incident incidente = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente no encontrado con ID: " + id));

        // Si ya está analizado con éxito, no repetir llamada innecesaria
        if ("ANALIZADO".equals(incidente.getEstadoAnalisisIA())) {
            return convertirADTO(incidente);
        }

        // Guardar estado PENDIENTE al iniciar
        incidente.setEstadoAnalisisIA("PENDIENTE");
        incidente.setFechaAnalisisIA(java.time.LocalDateTime.now());
        incidente.setModeloIA("gemini-2.5-flash");
        incidentRepository.saveAndFlush(incidente);

        try {
            // Obtener descripción original (si es nula, usar description)
            String desc = incidente.getDescripcionOriginal();
            if (desc == null || desc.trim().isEmpty()) {
                desc = incidente.getDescription();
            }

            GeminiIncidentAnalyzerService.IAAnalysisResult result =
                    geminiIncidentAnalyzerService.analizarIncidente(
                            incidente.getTitle(),
                            incidente.getType() != null ? incidente.getType().name() : "OTRO",
                            incidente.getSeverity(),
                            desc
                    );

            // Asignar los campos retornados por el servicio
            incidente.setTipoIncidenteIA(result.tipoIncidente);
            incidente.setPrioridadIA(result.prioridadIA);
            incidente.setResumenIA(result.resumenIA);
            incidente.setAccionSugeridaIA(result.accionSugeridaIA);
            incidente.setRequiereAtencionInmediata(result.requiereAtencionInmediata);
            incidente.setEstadoAnalisisIA("ANALIZADO");
            incidente.setFechaAnalisisIA(java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error al analizar incidente con Gemini para el ID: {}", id, e);
            incidente.setEstadoAnalisisIA("ERROR");
            incidente.setFechaAnalisisIA(java.time.LocalDateTime.now());
        }

        Incident saved = incidentRepository.save(incidente);
        return convertirADTO(saved);
    }
}
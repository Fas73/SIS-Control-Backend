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
    private AlertService alertService;

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
        entity.setSeverity(dto.getSeverity());
        entity.setImageUrl(dto.getImageUrl());
        entity.setType(tipoFinal);
        entity.setRoundExecution(round);
        entity.setChecklog(log);

        // Se envía a través de alertService para disparar los WebSockets/SSE en vivo hacia los paneles conectados
        Incident saved = alertService.registrarYDispararAlerta(entity);
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

    public IncidentDTO forzarMapeoDirecto(Incident i) {
        return this.convertirADTO(i);
    }

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

        if (i.getRoundExecution() != null) {
            dto.setRoundExecutionId(i.getRoundExecution().getId());

            if (i.getRoundExecution().getWorker() != null) {
                dto.setUsername(i.getRoundExecution().getWorker().getFullName());
            }
            if (i.getRoundExecution().getInstallation() != null) {
                dto.setClientName(i.getRoundExecution().getInstallation().getName());
            }
        }

        if (i.getChecklog() != null) {
            dto.setChecklogId(i.getChecklog().getId());

            if (i.getChecklog().getCheckpoint() != null) {
                com.siscontrol.backend.models.Checkpoint cp = i.getChecklog().getCheckpoint();

                dto.setCheckpointName(cp.getName());
                dto.setCheckpointOrder(cp.getExecutionOrder());

                if (cp.getNfcTagCode() != null && dto.getDescription() != null && !dto.getDescription().contains("NFC Tag:")) {
                    dto.setDescription(dto.getDescription() + "\nNFC Tag: " + cp.getNfcTagCode());
                }
            }
        }

        return dto;
    }
}
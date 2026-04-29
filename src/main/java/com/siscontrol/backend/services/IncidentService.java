package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.models.IncidentType;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.repositories.IncidentRepository;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.repositories.ChecklogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public IncidentDTO reportarIncidente(IncidentDTO dto) {
        // 1. Buscar entidades relacionadas
        RoundExecution round = roundExecutionRepository.findById(dto.getRoundExecutionId())
                .orElseThrow(() -> new RuntimeException("Ronda no encontrada"));

        Checklog log = null;
        if (dto.getChecklogId() != null) {
            log = checklogRepository.findById(dto.getChecklogId()).orElse(null);
        }

        // 2. Mapear DTO a Entidad
        Incident entity = new Incident();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setSeverity(dto.getSeverity());
        entity.setImageUrl(dto.getImageUrl());
        entity.setType(IncidentType.valueOf(dto.getType())); // Conversión String a Enum
        entity.setRoundExecution(round);
        entity.setChecklog(log);

        // 3. Guardar
        Incident saved = incidentRepository.save(entity);

        // 4. Retornar DTO convertido
        return new IncidentDTO(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getSeverity(),
                saved.getImageUrl(),
                saved.getType().name(),
                saved.getCreatedAt(),
                saved.getRoundExecution().getId(),
                (saved.getChecklog() != null ? saved.getChecklog().getId() : null)
        );
    }

    public List<IncidentDTO> obtenerTodos() {
        return incidentRepository.findAll().stream()
                .map(i -> new IncidentDTO(
                        i.getId(),
                        i.getTitle(),
                        i.getDescription(),
                        i.getSeverity(),
                        i.getImageUrl(),
                        i.getType() != null ? i.getType().name() : null,
                        i.getCreatedAt(),
                        i.getRoundExecution().getId(),
                        (i.getChecklog() != null ? i.getChecklog().getId() : null)
                ))
                .collect(Collectors.toList());
    }
}
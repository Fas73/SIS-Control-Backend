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

// --- REPORTAR INCIDENTE CON VALIDACIÓN DE ALTERNATIVAS ---
    public IncidentDTO reportarIncidente(IncidentDTO dto) {
        // 1. Validar el Enum antes de seguir
        IncidentType tipoFinal;
        try {
            tipoFinal = IncidentType.valueOf(dto.getType().toUpperCase().trim());
        } catch (Exception e) {
            // Si el tipo está mal, mostramos las alternativas reales del Enum
            String opciones = java.util.Arrays.toString(IncidentType.values());
            throw new IllegalArgumentException("Tipo de incidente inválido. Use uno de estos: " + opciones);
        }

        // 2. Buscar entidades relacionadas
        RoundExecution round = roundExecutionRepository.findById(dto.getRoundExecutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + dto.getRoundExecutionId()));

        Checklog log = null;
        if (dto.getChecklogId() != null) {
            log = checklogRepository.findById(dto.getChecklogId()).orElse(null);
        }

        // 3. Mapear DTO a Entidad
        Incident entity = new Incident();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setSeverity(dto.getSeverity());
        entity.setImageUrl(dto.getImageUrl());
        entity.setType(tipoFinal); // Usamos el tipo ya validado
        entity.setRoundExecution(round);
        entity.setChecklog(log);

        // 4. Guardar y retornar
        Incident saved = incidentRepository.save(entity);
        return convertirADTO(saved);
    }

    // --- LISTAR TODOS ---
    public List<IncidentDTO> obtenerTodos() {
        return incidentRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // --- NUEVO: OBTENER POR ID (Para solucionar el Build Error) ---
    public IncidentDTO obtenerPorId(Long id) {
        Incident incidente = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente no encontrado con ID: " + id));
        return convertirADTO(incidente);
    }

    // --- MÉTODO AUXILIAR DE MAPEÓ (Para no repetir código) ---
    private IncidentDTO convertirADTO(Incident i) {
        return new IncidentDTO(
                i.getId(),
                i.getTitle(),
                i.getDescription(),
                i.getSeverity(),
                i.getImageUrl(),
                i.getType() != null ? i.getType().name() : null,
                i.getCreatedAt(),
                i.getRoundExecution() != null ? i.getRoundExecution().getId() : null,
                (i.getChecklog() != null ? i.getChecklog().getId() : null)
        );
    }
}
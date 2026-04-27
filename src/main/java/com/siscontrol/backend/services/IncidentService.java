package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.repositories.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    public IncidentDTO reportarIncidente(IncidentDTO dto) {
        // Convertimos DTO a Entidad para guardar
        Incident entity = new Incident();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setSeverity(dto.getSeverity());
        entity.setImageUrl(dto.getImageUrl());

        Incident saved = incidentRepository.save(entity);

        // Retornamos el DTO con el ID generado
        return new IncidentDTO(saved.getId(), saved.getTitle(), saved.getDescription(),
                saved.getSeverity(), saved.getImageUrl(), saved.getCreatedAt());
    }

    public List<IncidentDTO> obtenerTodos() {
        return incidentRepository.findAll().stream()
                .map(i -> new IncidentDTO(i.getId(), i.getTitle(), i.getDescription(),
                        i.getSeverity(), i.getImageUrl(), i.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
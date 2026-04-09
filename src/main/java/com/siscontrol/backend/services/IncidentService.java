package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.repositories.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    // Guardar un incidente
    public Incident reportarIncidente(Incident incident) {
        // La fecha se genera sola gracias al @PrePersist que pusimos en el Model
        return incidentRepository.save(incident);
    }

    // Listar todos para el administrador
    public List<Incident> obtenerTodos() {
        return incidentRepository.findAll();
    }

    // Buscar por severidad (Ej: solo los "Altos")
    public List<Incident> buscarPorSeveridad(String severity) {
        return incidentRepository.findAll().stream()
                .filter(i -> i.getSeverity().equalsIgnoreCase(severity))
                .toList();
    }
}
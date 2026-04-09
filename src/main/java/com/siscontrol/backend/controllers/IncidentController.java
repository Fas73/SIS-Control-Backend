package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.services.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    // Endpoint para reportar: POST http://localhost:8080/api/incidents
    @PostMapping
    public Incident crearIncidente(@RequestBody Incident incident) {
        return incidentService.reportarIncidente(incident);
    }

    // Endpoint para ver todos: GET http://localhost:8080/api/incidents
    @GetMapping
    public List<Incident> listarTodo() {
        return incidentService.obtenerTodos();
    }
}
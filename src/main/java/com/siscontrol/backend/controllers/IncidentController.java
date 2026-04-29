package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.services.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentDTO> crearIncidente(@RequestBody IncidentDTO incidentDto) {
        return new ResponseEntity<>(incidentService.reportarIncidente(incidentDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IncidentDTO>> listarTodo() {
        List<IncidentDTO> incidentes = incidentService.obtenerTodos();
        return incidentes.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(incidentes);
    }
}
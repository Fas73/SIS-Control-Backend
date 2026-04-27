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
        IncidentDTO nuevoIncidente = incidentService.reportarIncidente(incidentDto);
        return new ResponseEntity<>(nuevoIncidente, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IncidentDTO>> listarTodo() {
        List<IncidentDTO> incidentes = incidentService.obtenerTodos();
        if (incidentes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(incidentes);
    }
}
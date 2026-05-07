package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.services.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    // POST http://localhost:8080/api/incidents
    @PostMapping
    public ResponseEntity<IncidentDTO> crearIncidente(@RequestBody IncidentDTO incidentDto) {
        return new ResponseEntity<>(incidentService.reportarIncidente(incidentDto), HttpStatus.CREATED);
    }

    // GET http://localhost:8080/api/incidents
    @GetMapping
    public ResponseEntity<?> listarTodo() {
        List<IncidentDTO> incidentes = incidentService.obtenerTodos();
        if (incidentes.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensaje", "No hay incidentes registrados en el sistema."));
        }
        return ResponseEntity.ok(incidentes);
    }

    // GET http://localhost:8080/api/incidents/1
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.obtenerPorId(id));
    }
}
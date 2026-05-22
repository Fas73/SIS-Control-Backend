package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.services.IncidentService;
import com.siscontrol.backend.services.AlertService;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private RoundExecutionRepository roundExecutionRepository;

    // POST http://localhost:8080/api/incidents
    @PostMapping
    public ResponseEntity<IncidentDTO> crearIncidente(@RequestBody IncidentDTO incidentDto) {
        return new ResponseEntity<>(incidentService.reportarIncidente(incidentDto), HttpStatus.CREATED);
    }

    // --- ENDPOINT CORREGIDO PARA EL BOTÓN DE PÁNICO MÓVIL ---
    // POST http://localhost:8080/api/incidents/panico
    @PostMapping("/panico")
    public ResponseEntity<?> dispararPanico(
            @RequestParam Long roundExecutionId,
            @RequestParam(required = false) String descripcion) {

        RoundExecution round = roundExecutionRepository.findById(roundExecutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + roundExecutionId));

        Incident panico = new Incident();
        panico.setTitle("ALERTA: BOTÓN DE PÁNICO ACTIVADO");
        panico.setDescription(descripcion != null && !descripcion.trim().isEmpty() ? descripcion : "Solicitud de ayuda inmediata de guardia en terreno.");
        panico.setSeverity("Alta"); // Pestaña de Pánico
        panico.setStatus(0); // Pendiente
        panico.setType(com.siscontrol.backend.enums.IncidentType.OTRO);
        panico.setRoundExecution(round);

        // Guarda en la base de datos tradicionales e irradia por WebSocket en vivo
        Incident guardado = alertService.registrarYDispararAlerta(panico);

        // Mapeamos al DTO unificado con todos los nombres resueltos para la respuesta de Retrofit
        return new ResponseEntity<>(incidentService.forzarMapeoDirecto(guardado), HttpStatus.CREATED);
    }

    // GET http://localhost:8080/api/incidents
    @GetMapping
    public ResponseEntity<?> listarTodo() {
        List<IncidentDTO> incidentes = incidentService.obtenerTodos();
        return ResponseEntity.ok(incidentes);
    }

    // GET http://localhost:8080/api/incidents/1
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.obtenerPorId(id));
    }
}
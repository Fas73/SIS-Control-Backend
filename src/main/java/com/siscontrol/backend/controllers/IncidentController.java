package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.services.IncidentService;
import com.siscontrol.backend.services.AlertService;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.models.Shift;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.repositories.ShiftRepository;
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

    @Autowired
    private ShiftRepository shiftRepository;

    // POST http://localhost:8080/api/incidents
    @PostMapping
    public ResponseEntity<IncidentDTO> crearIncidente(@RequestBody IncidentDTO incidentDto) {
        return new ResponseEntity<>(incidentService.reportarIncidente(incidentDto), HttpStatus.CREATED);
    }

    // --- ENDPOINT CORREGIDO PARA EL BOTÓN DE PÁNICO MÓVIL ---
    // POST http://localhost:8080/api/incidents/panico
    @PostMapping("/panico")
    public ResponseEntity<?> dispararPanico(
            @RequestParam(required = false) Long roundExecutionId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) String descripcion,
            @RequestBody(required = false) java.util.Map<String, Object> body) {

        Long rId = roundExecutionId;
        Long sId = shiftId;
        String desc = descripcion;

        if (body != null) {
            if (body.containsKey("roundExecutionId") && body.get("roundExecutionId") != null) {
                rId = Long.valueOf(body.get("roundExecutionId").toString());
            }
            if (body.containsKey("shiftId") && body.get("shiftId") != null) {
                sId = Long.valueOf(body.get("shiftId").toString());
            }
            if (body.containsKey("descripcion") && body.get("descripcion") != null) {
                desc = body.get("descripcion").toString();
            } else if (body.containsKey("description") && body.get("description") != null) {
                desc = body.get("description").toString();
            }
        }

        if (rId == null && sId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "El parámetro 'roundExecutionId' o 'shiftId' es obligatorio."));
        }

        Incident panico = new Incident();
        panico.setTitle("ALERTA: BOTÓN DE PÁNICO ACTIVADO");
        panico.setDescription(desc != null && !desc.trim().isEmpty() ? desc : "Solicitud de ayuda inmediata de guardia en terreno.");
        panico.setSeverity("Alta"); // Pestaña de Pánico
        panico.setStatus(0); // Pendiente

        if (rId != null) {
            final Long finalRId = rId;
            RoundExecution round = roundExecutionRepository.findById(finalRId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ronda no encontrada con ID: " + finalRId));
            panico.setType(com.siscontrol.backend.enums.IncidentType.OTRO);
            panico.setRoundExecution(round);
        } else if (sId != null) {
            final Long finalSId = sId;
            Shift shift = shiftRepository.findById(finalSId)
                    .orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada con ID: " + finalSId));
            panico.setType(com.siscontrol.backend.enums.IncidentType.JORNADA);
            panico.setShiftId(shift.getId());
        }

        // Guarda en la base de datos tradicionales e irradia por WebSocket en vivo
        Incident guardado = alertService.registrarYDispararAlerta(panico);

        // Mapeamos al DTO unificado con todos los nombres resueltos para la respuesta de Retrofit
        return new ResponseEntity<>(incidentService.forzarMapeoDirecto(guardado), HttpStatus.CREATED);
    }

    // GET http://localhost:8080/api/incidents
    @GetMapping
    public ResponseEntity<?> listarTodo(@RequestParam(required = false) Long supervisorId) {
        List<IncidentDTO> incidentes;
        if (supervisorId != null) {
            incidentes = incidentService.obtenerPorSupervisor(supervisorId);
        } else {
            incidentes = incidentService.obtenerTodos();
        }
        return ResponseEntity.ok(incidentes);
    }

    // GET http://localhost:8080/api/incidents/1
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.obtenerPorId(id));
    }
}
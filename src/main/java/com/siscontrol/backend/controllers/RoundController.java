package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.services.IncidentService;
import com.siscontrol.backend.services.RoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/rondas")
@CrossOrigin(origins = "*")
public class RoundController {

    @Autowired
    private RoundService roundService;

    @Autowired
    private IncidentService incidentService;

    // --- MÉTODOS DE JORNADA Y RONDA ---

    @PostMapping("/jornada/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarJornada(@RequestParam Long userId, @RequestParam Long installationId) {
        return new ResponseEntity<>(roundService.iniciarJornada(userId, installationId), HttpStatus.CREATED);
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarRonda(@RequestParam Long userId, @RequestParam Long installationId) {
        return new ResponseEntity<>(roundService.iniciarRonda(userId, installationId), HttpStatus.CREATED);
    }

    @PutMapping("/jornada/finalizar/{id}")
    public ResponseEntity<Map<String, Object>> finalizarJornada(@PathVariable Long id) {
        return ResponseEntity.ok(roundService.finalizarJornada(id));
    }

    @PutMapping("/finalizar/{id}")
    public ResponseEntity<Map<String, Object>> finalizarRonda(@PathVariable Long id) {
        return ResponseEntity.ok(roundService.finalizarRonda(id));
    }

    // --- MÉTODOS DE CANCELACIÓN ---

    @PutMapping("/jornada/cancelar/{id}")
    public ResponseEntity<Map<String, Object>> cancelarJornada(@PathVariable Long id, @RequestParam Long adminId) {
        return ResponseEntity.ok(roundService.cancelarJornada(id, adminId));
    }

    @PutMapping("/cancelar/{id}")
    public ResponseEntity<Map<String, Object>> cancelarRonda(@PathVariable Long id, @RequestParam Long adminId, @RequestParam String motivo) {
        return ResponseEntity.ok(roundService.cancelarRonda(id, adminId, motivo));
    }

    // --- CONSULTAS (Incluye nueva funcionalidad de filtro por Guardia) ---

    // URL: GET http://localhost:8080/api/rondas/buscar?userId=1
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarRondas(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Long installationId,
            @RequestParam(required = false) Long userId) {

        // Obtenemos el resultado como Object para evitar errores de tipos en el build
        Object resultado = roundService.filtrarRondas(fecha, installationId, userId);

        // Si es una lista y está vacía, devolvemos el mensaje personalizado para Postman
        if (resultado instanceof List) {
            List<?> lista = (List<?>) resultado;
            if (lista.isEmpty()) {
                return ResponseEntity.ok(Map.of("mensaje", "No se encontraron rondas para los criterios seleccionados."));
            }
        }

        return ResponseEntity.ok(resultado);
    }

    // URL: GET http://localhost:8080/api/rondas/1
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(roundService.obtenerDetalleRonda(id));
    }

    // --- REGISTROS (ESCANEOS E INCIDENCIAS) ---

    @PostMapping("/escaneo")
    public ResponseEntity<Map<String, Object>> realizarEscaneo(@RequestBody Checklog log) {
        return new ResponseEntity<>(roundService.registrarEscaneo(log), HttpStatus.CREATED);
    }

    @PostMapping("/incidente")
    public ResponseEntity<?> reportarIncidenteEnRonda(@RequestBody IncidentDTO dto) {
        return ResponseEntity.ok(incidentService.reportarIncidente(dto));
    }
}
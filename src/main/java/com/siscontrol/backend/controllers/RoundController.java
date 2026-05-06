package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.dto.StartRoundRequestDTO;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.services.RoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rondas")
public class RoundController {

    @Autowired
    private RoundService roundService;

    // --- Endpoints Incidentes ---

    @PostMapping("/incidente")
    public ResponseEntity<Incident> reportarIncidente(@RequestBody Incident incident) {
        return new ResponseEntity<>(roundService.registrarIncidente(incident), HttpStatus.CREATED);
    }

    @GetMapping("/incidente/{roundExecutionId}")
    public ResponseEntity<List<Incident>> listarIncidentes(@PathVariable Long roundExecutionId) {
        return ResponseEntity.ok(roundService.obtenerIncidentesPorRonda(roundExecutionId));
    }

    // --- Endpoints Rondas y Operaciones ---

    @PostMapping("/iniciar")
    public ResponseEntity<RoundExecution> iniciarRonda(@RequestBody StartRoundRequestDTO request) {
        return new ResponseEntity<>(roundService.iniciarRonda(request), HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<RoundExecution>> listarRondas() {
        return ResponseEntity.ok(roundService.obtenerTodasLasRondas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoundExecution> obtenerRonda(@PathVariable Long id) {
        return ResponseEntity.ok(roundService.obtenerRondaPorId(id));
    }

    @PutMapping("/finalizar/{id}")
    public ResponseEntity<RoundExecution> finalizarRonda(@PathVariable Long id) {
        return ResponseEntity.ok(roundService.finalizarRonda(id));
    }

    // --- Endpoints Instalaciones ---

    @PostMapping("/instalaciones")
    public ResponseEntity<Installation> crearInstalacion(@RequestBody Installation installation) {
        return new ResponseEntity<>(roundService.guardarInstalacion(installation), HttpStatus.CREATED);
    }

    @GetMapping("/instalaciones")
    public ResponseEntity<List<Installation>> listarInstalaciones() {
        return ResponseEntity.ok(roundService.obtenerTodasLasInstalaciones());
    }

    // --- Endpoints Checkpoints ---

    @PostMapping("/checkpoints")
    public ResponseEntity<Checkpoint> crearCheckpoint(@RequestBody Checkpoint checkpoint) {
        return new ResponseEntity<>(roundService.guardarCheckpoint(checkpoint), HttpStatus.CREATED);
    }

    @GetMapping("/checkpoints/{installationId}")
    public ResponseEntity<List<CheckpointDTO>> listarCheckpoints(@PathVariable Long installationId) {
        List<CheckpointDTO> checkpoints = roundService.obtenerCheckpointsPorInstalacion(installationId);
        if (checkpoints.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(checkpoints);
    }

    @PostMapping("/escaneo")
    public ResponseEntity<Checklog> realizarEscaneo(@RequestBody Checklog log) {
        return new ResponseEntity<>(roundService.registrarEscaneo(log), HttpStatus.CREATED);
    }

    // --- Manejador de Errores Global ---

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
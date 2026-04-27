package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.services.RoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rondas")
public class RoundController {

    @Autowired
    private RoundService roundService;

    @PostMapping("/instalaciones")
    public ResponseEntity<Installation> crearInstalacion(@RequestBody Installation installation) {
        return new ResponseEntity<>(roundService.guardarInstalacion(installation), HttpStatus.CREATED);
    }

    @GetMapping("/instalaciones")
    public ResponseEntity<List<Installation>> listarInstalaciones() {
        return ResponseEntity.ok(roundService.obtenerTodasLasInstalaciones());
    }

    @PostMapping("/checkpoints")
    public ResponseEntity<Checkpoint> crearCheckpoint(@RequestBody Checkpoint checkpoint) {
        return new ResponseEntity<>(roundService.guardarCheckpoint(checkpoint), HttpStatus.CREATED);
    }

    @GetMapping("/checkpoints/{installationId}")
    public ResponseEntity<List<CheckpointDTO>> listarCheckpoints(@PathVariable Long installationId) {
        List<CheckpointDTO> checkpoints = roundService.obtenerCheckpointsPorInstalacion(installationId);
        if (checkpoints.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(checkpoints);
    }

    @PostMapping("/escaneo")
    public ResponseEntity<Checklog> realizarEscaneo(@RequestBody Checklog log) {
        return new ResponseEntity<>(roundService.registrarEscaneo(log), HttpStatus.CREATED);
    }
}
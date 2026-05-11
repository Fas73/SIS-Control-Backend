package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.services.InstallationService;
import com.siscontrol.backend.services.CheckpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/instalaciones")
@CrossOrigin(origins = "*")
public class InstallationController {

    @Autowired private InstallationService installationService;
    @Autowired private CheckpointService checkpointService;

    @GetMapping
    public ResponseEntity<?> listarInstalaciones() {
        // Ahora devuelve 200 OK con la lista O con el mensaje de "No existen instalaciones"
        return ResponseEntity.ok(installationService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearInstalacion(@RequestBody Installation installation) {
        return new ResponseEntity<>(installationService.guardarInstalacion(installation), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Installation> actualizar(
            @PathVariable Long id,
            @RequestParam Long editorId,
            @RequestBody Installation inst) {
        return ResponseEntity.ok(installationService.actualizar(editorId, id, inst));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable Long id,
            @RequestParam Long editorId) {
        installationService.eliminarLogico(editorId, id);
        return ResponseEntity.ok(Map.of("mensaje", "Instalación desactivada correctamente"));
    }

    // --- CHECKPOINTS (NFC) ---
    @PostMapping("/checkpoints")
    public ResponseEntity<Map<String, Object>> crearCheckpoint(@RequestBody Checkpoint checkpoint) {
        return new ResponseEntity<>(checkpointService.guardarCheckpoint(checkpoint), HttpStatus.CREATED);
    }

    @GetMapping("/{installationId}/checkpoints")
    public ResponseEntity<?> listarCheckpoints(@PathVariable Long installationId) {
        // Usamos ResponseEntity<?> para mayor flexibilidad en la respuesta
        return ResponseEntity.ok(checkpointService.obtenerPorInstalacion(installationId));
    }
}
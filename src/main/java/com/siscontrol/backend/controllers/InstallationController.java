package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.Checkpoint;
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

    @Autowired
    private InstallationService installationService;

    @Autowired
    private CheckpointService checkpointService;

    // ==========================================
    // --- MÉTODOS DE INSTALACIONES ---
    // ==========================================

    @GetMapping
    public ResponseEntity<?> listarInstalaciones() {
        return ResponseEntity.ok(installationService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearInstalacion(
            @RequestParam Long editorId,
            @RequestBody Installation installation) {
        return new ResponseEntity<>(installationService.guardarInstalacion(editorId, installation), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Installation> actualizarInstalacion(
            @PathVariable Long id,
            @RequestParam Long editorId,
            @RequestBody Installation inst) {
        return ResponseEntity.ok(installationService.actualizar(editorId, id, inst));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> alternarEstadoInstalacion(
            @PathVariable Long id,
            @RequestParam Long editorId) {

        Installation actualizada = installationService.alternarEstado(editorId, id);
        String nuevoEstado = actualizada.getStatus() == 1 ? "Activo" : "Inactivo";

        return ResponseEntity.ok(Map.of(
                "mensaje", "Estado actualizado a " + nuevoEstado,
                "status", actualizada.getStatus()
        ));
    }

    // ==========================================
    // --- MÉTODOS DE CHECKPOINTS (NFC) ---
    // ==========================================

    @PostMapping("/checkpoints")
    public ResponseEntity<Map<String, Object>> crearCheckpoint(
            @RequestParam Long editorId,
            @RequestBody Checkpoint checkpoint) {
        return new ResponseEntity<>(checkpointService.guardarCheckpoint(editorId, checkpoint), HttpStatus.CREATED);
    }

    @PutMapping("/checkpoints/{id}")
    public ResponseEntity<?> actualizarCheckpoint(
            @PathVariable Long id,
            @RequestParam Long editorId,
            @RequestBody Checkpoint checkpoint) {
        return ResponseEntity.ok(checkpointService.actualizar(editorId, id, checkpoint));
    }

    @GetMapping("/{installationId}/checkpoints")
    public ResponseEntity<?> listarCheckpoints(@PathVariable Long installationId) {
        return ResponseEntity.ok(checkpointService.obtenerPorInstalacion(installationId));
    }

    @PatchMapping("/checkpoints/{id}/toggle-status")
    public ResponseEntity<?> alternarEstadoCheckpoint(
            @PathVariable Long id,
            @RequestParam Long editorId) {

        Checkpoint actualizado = checkpointService.alternarEstado(editorId, id);
        String estadoTxt = actualizado.getStatus() == 1 ? "Activo" : "Inactivo";

        return ResponseEntity.ok(Map.of(
                "mensaje", "Estado del checkpoint actualizado a " + estadoTxt,
                "status", actualizado.getStatus(),
                "id", actualizado.getId()
        ));
    }
}
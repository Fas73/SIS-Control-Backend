package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.dto.RoundHistoryItemDTO;
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

    @Autowired private RoundService roundService;

    // --- CONTROL DE RONDAS ---

    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarRonda(
            @RequestParam Long userId,
            @RequestParam Long installationId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return new ResponseEntity<>(roundService.iniciarRonda(userId, installationId, latitude, longitude), HttpStatus.CREATED);
    }

    @PutMapping("/finalizar/{id}")
    public ResponseEntity<?> finalizarRonda(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String obs = (body != null) ? body.getOrDefault("observations", "Completada") : "Completada";
        return ResponseEntity.ok(roundService.finalizarRonda(id, obs));
    }

    @PutMapping("/cancelar/{id}")
    public ResponseEntity<?> cancelarRonda(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(roundService.cancelarRondaAdministrativamente(id, adminId, motivo));
    }

    @PutMapping("/jornada/cancelar/{id}")
    public ResponseEntity<?> cancelarJornada(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ResponseEntity.ok(roundService.cancelarJornadaAdministrativamente(id, adminId));
    }

    // --- ESCANEOS E INCIDENTES ---

    @PostMapping("/escaneo")
    public ResponseEntity<?> realizarEscaneo(@RequestBody Checklog log) {
        return new ResponseEntity<>(roundService.registrarEscaneo(log), HttpStatus.CREATED);
    }

    // --- CONSULTAS ---

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarRondas(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Long installationId,
            @RequestParam(required = false) Long userId) {
        List<?> lista = roundService.filtrarRondas(fecha, installationId, userId);
        return ResponseEntity.ok(lista);
    }

    // --- CAMBIO: SE AGREGA REQUESTER ID COMO PARÁMETRO OBLIGATORIO ---
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerDetalle(
            @PathVariable Long id,
            @RequestParam Long requesterId) {
        return ResponseEntity.ok(roundService.obtenerDetalleRonda(id, requesterId));
    }

    // --- RECUPERACIÓN DE ESTADO PARA LA APP MÓVIL ---
    @GetMapping("/estado-actual/{userId}")
    public ResponseEntity<?> obtenerEstadoActual(@PathVariable Long userId) {
        return ResponseEntity.ok(roundService.verificarEstadoActual(userId));
    }
}
package com.siscontrol.backend.controllers;

import com.siscontrol.backend.dto.CheckInRequestDTO;
import com.siscontrol.backend.dto.CheckOutRequestDTO;
import com.siscontrol.backend.services.InstallationService;
import com.siscontrol.backend.services.RoundService;
import com.siscontrol.backend.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asistencia")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired private InstallationService installationService;
    @Autowired private RoundService roundService;

    @PostMapping("/check-in")
    public ResponseEntity<?> registrarEntrada(@RequestBody CheckInRequestDTO request) {
        validarUbicacion(request.getInstallationId(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(roundService.iniciarJornada(request.getUserId(), request.getInstallationId()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<?> registrarSalida(@RequestBody CheckOutRequestDTO request) {
        validarUbicacion(request.getInstallationId(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(roundService.finalizarJornada(request.getUserId(), request.getInstallationId()));
    }

    private void validarUbicacion(Long instId, Double lat, Double lon) {
        if (!installationService.verificarUbicacion(instId, lat, lon)) {
            throw new ForbiddenException("Error de GPS: Debes estar en la instalación para realizar esta acción.");
        }
    }
}
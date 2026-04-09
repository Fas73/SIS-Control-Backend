package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.services.RoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rondas")
public class RoundController {

    @Autowired
    private RoundService roundService;

    // Crear Instalación: POST http://localhost:8080/api/rondas/instalaciones
    @PostMapping("/instalaciones")
    public Installation crearInstalacion(@RequestBody Installation installation) {
        return roundService.guardarInstalacion(installation);
    }

    // Ver todas las instalaciones: GET http://localhost:8080/api/rondas/instalaciones
    @GetMapping("/instalaciones")
    public List<Installation> listarInstalaciones() {
        return roundService.obtenerTodasLasInstalaciones();
    }

    // Crear Punto de Control: POST http://localhost:8080/api/rondas/checkpoints
    @PostMapping("/checkpoints")
    public Checkpoint crearCheckpoint(@RequestBody Checkpoint checkpoint) {
        return roundService.guardarCheckpoint(checkpoint);
    }

    @PostMapping("/escaneo")
    public Checklog realizarEscaneo(@RequestBody Checklog log) {
        return roundService.registrarEscaneo(log);
    }
}
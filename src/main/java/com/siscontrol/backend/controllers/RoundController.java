package com.siscontrol.backend.controllers;

import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.CheckpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rondas")
public class RoundController {

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private CheckpointRepository checkpointRepository;

    // Obtener todas las instalaciones
    @GetMapping("/instalaciones")
    public List<Installation> getInstalaciones() {
        return installationRepository.findAll();
    }

    // Crear una nueva instalación
    @PostMapping("/instalaciones")
    public Installation crearInstalacion(@RequestBody Installation installation) {
        return installationRepository.save(installation);
    }

    // Crear un punto de control
    @PostMapping("/checkpoints")
    public Checkpoint crearCheckpoint(@RequestBody Checkpoint checkpoint) {
        return checkpointRepository.save(checkpoint);
    }
}
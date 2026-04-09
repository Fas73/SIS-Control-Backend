package com.siscontrol.backend.services;

import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class RoundService {

    @Autowired
    private ChecklogRepository checklogRepository;

    @Autowired
    private RoundExecutionRepository executionRepository;

    // Método para registrar que el guardia pasó por un punto
    public Checklog registrarPasoPorPunto(RoundExecution execution, Checkpoint checkpoint) {
        Checklog nuevoLog = new Checklog();
        nuevoLog.setTimestamp(LocalDateTime.now());
        nuevoLog.setRoundExecution(execution);
        nuevoLog.setCheckpoint(checkpoint);

        return checklogRepository.save(nuevoLog);
    }
}
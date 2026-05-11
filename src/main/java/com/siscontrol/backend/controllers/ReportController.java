package com.siscontrol.backend.controllers;

import com.siscontrol.backend.services.RoundService;
import com.siscontrol.backend.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired private RoundService roundService;
    @Autowired private ReportService reportService;

    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(roundService.obtenerEstadisticasGlobales(inicio, fin));
    }

    @GetMapping("/rondas/all")
    public ResponseEntity<?> listarRondas(@RequestParam Long requesterId) {
        return ResponseEntity.ok(reportService.obtenerHistorialRondas(requesterId));
    }

    @GetMapping("/jornadas/all")
    public ResponseEntity<?> listarJornadas(@RequestParam Long requesterId) {
        return ResponseEntity.ok(reportService.obtenerHistorialJornadas(requesterId));
    }
}
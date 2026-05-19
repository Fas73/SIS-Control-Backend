package com.siscontrol.backend.controllers;

import com.siscontrol.backend.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import com.siscontrol.backend.dto.AdminDashboardDTO;
import com.siscontrol.backend.dto.DashboardActiveRoundDTO;
import com.siscontrol.backend.dto.DashboardActiveShiftDTO;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired private ReportService reportService;

    // 1. Estadísticas globales del Dashboard (Admin/Supervisor)
    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        // CORREGIDO: Llamada limpia a un solo nivel de reportService
        return ResponseEntity.ok(reportService.obtenerEstadisticasGlobales(inicio, fin));
    }

    // 2. Consulta de cantidad de Guardias activos y totales
    @GetMapping("/guardias")
    public ResponseEntity<?> obtenerEstadisticasGuardias() {
        return ResponseEntity.ok(reportService.obtenerEstadisticasGuardias());
    }

    // 3. Listar historial crudo de rondas
    @GetMapping("/rondas/all")
    public ResponseEntity<?> listarRondas(@RequestParam Long requesterId) {
        return ResponseEntity.ok(reportService.obtenerHistorialRondas(requesterId));
    }

    // 4. Listar historial crudo de jornadas
    @GetMapping("/jornadas/all")
    public ResponseEntity<?> listarJornadas(@RequestParam Long requesterId) {
        return ResponseEntity.ok(reportService.obtenerHistorialJornadas(requesterId));
    }

    // 5. Endpoint optimizado con Datos Reales para la App Móvil
    @GetMapping("/mis-rondas/{guardId}")
    public ResponseEntity<?> obtenerMisRondas(
            @PathVariable Long guardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(reportService.obtenerHistorialRondasGuardia(guardId, inicio, fin));
    }

    // ... dentro de ReportController.java ...

    @GetMapping("/dashboard-admin")
    public ResponseEntity<AdminDashboardDTO> obtenerDashboardAdmin() {
        return ResponseEntity.ok(reportService.obtenerDashboardAdmin());
    }

}
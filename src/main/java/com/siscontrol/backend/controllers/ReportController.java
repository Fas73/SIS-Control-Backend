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
import com.siscontrol.backend.services.CsvReportService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired private ReportService reportService;
    @Autowired private CsvReportService csvReportService;

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

    // --- REPORTES CSV ---

    @PostMapping("/csv/generar")
    public ResponseEntity<?> generarReporteCsv() {
        try {
            return ResponseEntity.ok(csvReportService.generarReporteRondasCsv());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/csv/descargar/{fileName}")
    public ResponseEntity<?> descargarReporteCsv(@PathVariable String fileName) {
        // Validación de seguridad estricta para evitar Path Traversal
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre de archivo inválido o peligroso."));
        }

        try {
            Path filePath = Paths.get("reportes").resolve(fileName).normalize();
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.status(404).body(Map.of("error", "El archivo solicitado no existe."));
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al descargar el archivo: " + e.getMessage()));
        }
    }

    @GetMapping("/csv/listar")
    public ResponseEntity<?> listarReportesCsv() {
        try {
            File folder = new File("reportes");
            if (!folder.exists()) {
                return ResponseEntity.ok(List.of());
            }

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files == null) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> fileList = java.util.Arrays.stream(files)
                    .map(file -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("fileName", file.getName());
                        map.put("size", file.length());
                        map.put("lastModified", file.lastModified());
                        map.put("downloadUrl", "/api/reportes/csv/descargar/" + file.getName());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(fileList);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al listar los archivos: " + e.getMessage()));
        }
    }

}
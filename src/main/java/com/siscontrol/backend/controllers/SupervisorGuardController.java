package com.siscontrol.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.siscontrol.backend.services.SupervisorGuardService;

@RestController
@RequestMapping("/api/supervisor-guard")
@CrossOrigin(origins = "*")
public class SupervisorGuardController {

    @Autowired
    private SupervisorGuardService supervisorGuardService;

    /**
     * ASIGNAR UN GUARDIA A UN SUPERVISOR
     * Ahora devuelve ResponseEntity<Object> para incluir el mensaje de éxito.
     * Endpoint: POST http://localhost:8080/api/supervisor-guard?supervisorId=3&guardId=2
     */
    @PostMapping
    public ResponseEntity<Object> asignarGuardia(
            @RequestParam Long supervisorId,
            @RequestParam Long guardId
    ) {
        // Retornamos Object porque el Service ahora devuelve un Map con "mensaje" y "data"
        return ResponseEntity.ok(supervisorGuardService.asignarGuardia(supervisorId, guardId));
    }

    /**
     * LISTAR GUARDIAS DE UN SUPERVISOR
     * Endpoint: GET http://localhost:8080/api/supervisor-guard/3
     */
    @GetMapping("/{supervisorId}")
    public ResponseEntity<Object> obtenerGuardiasDeSupervisor(@PathVariable Long supervisorId) {
        return ResponseEntity.ok(supervisorGuardService.obtenerGuardiasDeSupervisor(supervisorId));
    }

    /**
     * ELIMINAR ASIGNACIÓN
     * Endpoint: DELETE http://localhost:8080/api/supervisor-guard?supervisorId=3&guardId=2
     */
    @DeleteMapping
    public ResponseEntity<Object> eliminarAsignacion(
            @RequestParam Long supervisorId,
            @RequestParam Long guardId
    ) {
        supervisorGuardService.eliminarAsignacion(supervisorId, guardId);
        return ResponseEntity.ok("Asignación eliminada correctamente.");
    }
}
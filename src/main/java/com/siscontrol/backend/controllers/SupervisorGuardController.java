package com.siscontrol.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.siscontrol.backend.models.SupervisorGuard;
import com.siscontrol.backend.services.SupervisorGuardService;

@RestController
@RequestMapping("/api/supervisor-guard")
public class SupervisorGuardController {

    @Autowired
    private SupervisorGuardService supervisorGuardService;

    @PostMapping
    public SupervisorGuard asignarGuardia(
            @RequestParam Long supervisorId,
            @RequestParam Long guardId
    ) {
        return supervisorGuardService.asignarGuardiaASupervisor(supervisorId, guardId);
    }

    @GetMapping("/{supervisorId}")
    public List<SupervisorGuard> obtenerGuardiasDeSupervisor(@PathVariable Long supervisorId) {
        return supervisorGuardService.obtenerGuardiasDeSupervisor(supervisorId);
    }



}

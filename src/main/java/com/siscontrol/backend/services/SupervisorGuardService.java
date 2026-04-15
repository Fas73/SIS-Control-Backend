package com.siscontrol.backend.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siscontrol.backend.dto.SupervisorGuardResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.SupervisorGuard;
import com.siscontrol.backend.models.SupervisorGuardId;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.SupervisorGuardRepository;
import com.siscontrol.backend.repositories.UserRepository;

@Service
public class SupervisorGuardService {

    @Autowired
    private SupervisorGuardRepository supervisorGuardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    public SupervisorGuard asignarGuardia(Long supervisorId, Long guardId) {
        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
        
        User guard = userRepository.findById(guardId)
                .orElseThrow(() -> new RuntimeException("Guardia no encontrado"));

        if (supervisor.getRole() != UserRole.SUPERVISOR) {
            throw new RuntimeException("El usuario asignado no es un supervisor");
        }
        
        if (guard.getRole() != UserRole.GUARD) {
            throw new RuntimeException("El usuario asignado no es una guardia");
        }

        SupervisorGuard relacion = new SupervisorGuard();
        relacion.setId(new SupervisorGuardId(supervisorId, guardId));
        relacion.setSupervisor(supervisor);
        relacion.setGuard(guard);

        return supervisorGuardRepository.save(relacion);
    }

    public List<SupervisorGuardResponseDTO> obtenerGuardiasDeSupervisor(Long supervisorId) {
        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
        
        return supervisorGuardRepository.findBySupervisor(supervisor)
                .stream()
                .map(relacion -> new SupervisorGuardResponseDTO(
                        userService.convertirAResponseDTO(relacion.getSupervisor()),
                        userService.convertirAResponseDTO(relacion.getGuard())
                ))
                .toList();
    }

    

}

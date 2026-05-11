package com.siscontrol.backend.services;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siscontrol.backend.dto.SupervisorGuardResponseDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.SupervisorGuard;
import com.siscontrol.backend.models.SupervisorGuardId;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.SupervisorGuardRepository;
import com.siscontrol.backend.repositories.UserRepository;
import com.siscontrol.backend.exception.BadRequestException;
import com.siscontrol.backend.exception.ResourceNotFoundException;

@Service
public class SupervisorGuardService {

    @Autowired
    private SupervisorGuardRepository supervisorGuardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * ASIGNAR GUARDIA A SUPERVISOR
     * Devuelve un mensaje de éxito y la data de la relación.
     */
    public Object asignarGuardia(Long supervisorId, Long guardId) {
        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor no encontrado"));

        User guard = userRepository.findById(guardId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardia no encontrado"));

        if (supervisor.getRole() != UserRole.SUPERVISOR) {
            throw new BadRequestException("El usuario asignado no es un supervisor");
        }

        if (guard.getRole() != UserRole.GUARD) {
            throw new BadRequestException("El usuario asignado no es un guardia");
        }

        SupervisorGuardId relationId = new SupervisorGuardId(supervisorId, guardId);

        if (supervisorGuardRepository.existsById(relationId)) {
            throw new BadRequestException("La relación supervisor-guardia ya existe");
        }

        SupervisorGuard relacion = new SupervisorGuard();
        relacion.setId(relationId);
        relacion.setSupervisor(supervisor);
        relacion.setGuard(guard);

        SupervisorGuard savedRelation = supervisorGuardRepository.save(relacion);

        // Estructura de respuesta con mensaje de éxito
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Guardia " + guard.getFullName() + " asignado exitosamente al supervisor " + supervisor.getFullName());
        response.put("data", new SupervisorGuardResponseDTO(
                userService.convertirAResponseDTO(savedRelation.getSupervisor()),
                userService.convertirAResponseDTO(savedRelation.getGuard())
        ));

        return response;
    }

    /**
     * OBTENER GUARDIAS (Con mensaje si la lista está vacía)
     */
    public Object obtenerGuardiasDeSupervisor(Long supervisorId) {
        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor no encontrado"));

        List<SupervisorGuardResponseDTO> guardias = supervisorGuardRepository.findBySupervisor(supervisor)
                .stream()
                .map(relacion -> new SupervisorGuardResponseDTO(
                        userService.convertirAResponseDTO(relacion.getSupervisor()),
                        userService.convertirAResponseDTO(relacion.getGuard())
                ))
                .toList();

        if (guardias.isEmpty()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mensaje", "El supervisor " + supervisor.getFullName() + " aún no tiene guardias asignados.");
            response.put("guardias", guardias);
            response.put("total", 0);
            return response;
        }

        return guardias;
    }

    /**
     * ELIMINAR ASIGNACIÓN
     */
    public void eliminarAsignacion(Long supervisorId, Long guardId) {
        SupervisorGuardId relationId = new SupervisorGuardId(supervisorId, guardId);

        if (!supervisorGuardRepository.existsById(relationId)) {
            throw new ResourceNotFoundException("No existe una relación entre el supervisor ID " + supervisorId + " y el guardia ID " + guardId);
        }

        supervisorGuardRepository.deleteById(relationId);
    }
}
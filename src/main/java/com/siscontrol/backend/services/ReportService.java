package com.siscontrol.backend.services;

import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ReportService {

    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;

    public Object obtenerHistorialJornadas(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Shift> jornadas = (requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.SUPERVISOR)
                ? shiftRepository.findAll()
                : shiftRepository.findByWorkerId(requesterId);

        return validarRespuesta(jornadas, "No se encontraron registros de jornadas.");
    }

    public Object obtenerHistorialRondas(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<RoundExecution> rondas = (requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.SUPERVISOR)
                ? roundExecutionRepository.findAll()
                : roundExecutionRepository.findByWorkerId(requesterId);

        return validarRespuesta(rondas, "No se encontraron registros de rondas.");
    }

    private Object validarRespuesta(List<?> lista, String mensaje) {
        if (lista.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", 0);
            res.put("mensaje", mensaje);
            return res;
        }
        return lista;
    }
}
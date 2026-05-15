package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.RoundStatus;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.*;
import com.siscontrol.backend.repositories.*;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired private RoundExecutionRepository roundExecutionRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;

    // 1. Método de estadísticas que consultaste
    public Map<String, Object> obtenerEstadisticasGlobales(LocalDateTime inicio, LocalDateTime fin) {
        List<RoundExecution> todas = roundExecutionRepository.findAll();

        List<RoundExecution> filtradas = todas.stream()
                .filter(r -> (inicio == null || !r.getStartTime().isBefore(inicio)) &&
                        (fin == null || !r.getStartTime().isAfter(fin)))
                .collect(Collectors.toList());

        long completadas = filtradas.stream().filter(r -> r.getStatus() == RoundStatus.FINALIZADA).count();
        long enProgreso = filtradas.stream().filter(r -> r.getStatus() == RoundStatus.EN_PROGRESO).count();

        return Map.of(
                "totalRondas", filtradas.size(),
                "completadas", completadas,
                "enProgreso", enProgreso,
                "periodo", Map.of("desde", inicio != null ? inicio : "inicio", "hasta", fin != null ? fin : "ahora")
        );
    }

    // 2. Historial de Jornadas (Asistencia)
    public Object obtenerHistorialJornadas(Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Shift> jornadas = (requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.SUPERVISOR)
                ? shiftRepository.findAll()
                : shiftRepository.findByWorkerId(requesterId);

        return validarRespuesta(jornadas, "No se encontraron registros de jornadas.");
    }

    // 3. Historial de Rondas
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
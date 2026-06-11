package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.AlertNotificationDTO;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.models.Shift;
import com.siscontrol.backend.repositories.IncidentRepository;
import com.siscontrol.backend.repositories.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AlertService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private ShiftRepository shiftRepository;

    @Transactional
    public Incident registrarYDispararAlerta(Incident incidente) {
        if (incidente.getCreatedAt() == null) {
            incidente.setCreatedAt(LocalDateTime.now());
        }

        // 1. Persistencia: Guarda en la tabla 'incidents'
        Incident guardado = incidentRepository.save(incidente);

        // 2. Mapeo dinámico y cruce de datos para el tiempo real
        AlertNotificationDTO dto = new AlertNotificationDTO();
        dto.setId(guardado.getId());
        dto.setTitle(guardado.getTitle());
        dto.setDescription(guardado.getDescription());
        dto.setSeverity(guardado.getSeverity());
        dto.setType(guardado.getType() != null ? guardado.getType().name() : "OTRO");
        dto.setCreatedAt(guardado.getCreatedAt());
        dto.setStatus(guardado.getStatus());

        // Extraer datos de la ronda y del guardia
        if (guardado.getRoundExecution() != null) {
            dto.setRoundExecutionId(guardado.getRoundExecution().getId());
            try {
                if (guardado.getRoundExecution().getWorker() != null) {
                    dto.setUsername(guardado.getRoundExecution().getWorker().getFullName());
                    dto.setWorkerId(guardado.getRoundExecution().getWorker().getId());
                }
            } catch (Exception e) {
                dto.setUsername("Desconocido");
            }

            // Cruzar datos con la tabla 'installations' para obtener el client_name
            try {
                if (guardado.getRoundExecution().getInstallation() != null) {
                    dto.setClientName(guardado.getRoundExecution().getInstallation().getClientName());
                }
            } catch (Exception e) {
                dto.setClientName("Desconocida");
            }
        } else if (guardado.getShiftId() != null) {
            dto.setRoundExecutionId(guardado.getShiftId());
            Shift shift = shiftRepository.findById(guardado.getShiftId()).orElse(null);
            if (shift != null) {
                try {
                    if (shift.getWorker() != null) {
                        dto.setUsername(shift.getWorker().getFullName());
                        dto.setWorkerId(shift.getWorker().getId());
                    }
                } catch (Exception e) {
                    dto.setUsername("Desconocido");
                }
                try {
                    if (shift.getInstallation() != null) {
                        dto.setClientName(shift.getInstallation().getName());
                    }
                } catch (Exception e) {
                    dto.setClientName("Desconocida");
                }
            }
        }

        // === REESTRUCTURACIÓN DE CHECKLOG Y FOTO (CORREGIDO) ===
        if (guardado.getChecklog() != null) {
            // Asignamos la foto del escaneo que acabas de habilitar
            dto.setChecklogImageUrl(guardado.getChecklog().getImageUrl());

            // Si el escaneo tiene un checkpoint físico, extraemos sus metadatos
            if (guardado.getChecklog().getCheckpoint() != null) {
                dto.setCheckpointName(guardado.getChecklog().getCheckpoint().getName());
                dto.setExecutionOrder(guardado.getChecklog().getCheckpoint().getExecutionOrder());
            } else {
                dto.setCheckpointName("N/A");
                dto.setExecutionOrder(0);
            }
        } else {
            // Valores por defecto si la alerta no proviene de un escaneo (ej. Botón de pánico)
            dto.setCheckpointName("N/A");
            dto.setExecutionOrder(0);
            dto.setChecklogImageUrl(null);
        }

        // Si el cliente no se logró extraer de la instalación (ej. un incidente aislado), asegurar valor
        if (dto.getClientName() == null && guardado.getRoundExecution() != null
                && guardado.getRoundExecution().getInstallation() != null) {
            dto.setClientName(guardado.getRoundExecution().getInstallation().getName());
        }

        // 3. Tiempo Real: Envía el DTO aplanado con todos los nombres resueltos
        messagingTemplate.convertAndSend("/topic/alertas", dto);

        return guardado;
    }

    // 🔥 NUEVO MÉTODO: Envía notificaciones globales de sistema directo al WebSocket sin persistir incidentes
    public void dispararNotificacionDirecta(String titulo, String descripcion, String username, Long workerId, Long shiftId) {
        AlertNotificationDTO dto = new AlertNotificationDTO();
        dto.setId(0L); // ID de sistema
        dto.setTitle(titulo);
        dto.setDescription(descripcion);
        dto.setSeverity("Alta");
        dto.setType("OTRO");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setStatus(1);
        dto.setUsername(username);
        dto.setCheckpointName("N/A");
        dto.setExecutionOrder(0);
        dto.setClientName("Sistema");
        dto.setChecklogImageUrl(null);
        dto.setWorkerId(workerId);
        dto.setRoundExecutionId(shiftId);

        // Envía el DTO estructurado idéntico al que espera Android
        messagingTemplate.convertAndSend("/topic/alertas", dto);
    }
}
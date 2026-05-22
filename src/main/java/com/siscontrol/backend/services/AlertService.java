package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.AlertNotificationDTO;
import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.repositories.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AlertService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private IncidentRepository incidentRepository;

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
            if (guardado.getRoundExecution().getWorker() != null) {
                dto.setUsername(guardado.getRoundExecution().getWorker().getFullName());
            }

            // Cruzar datos con la tabla 'installations' para obtener el client_name
            if (guardado.getRoundExecution().getInstallation() != null) {
                dto.setClientName(guardado.getRoundExecution().getInstallation().getClientName());
            }
        }

        // Cruzar datos con la tabla 'checkpoints' a través del checklog asociado
        if (guardado.getChecklog() != null && guardado.getChecklog().getCheckpoint() != null) {
            dto.setCheckpointName(guardado.getChecklog().getCheckpoint().getName());
            dto.setExecutionOrder(guardado.getChecklog().getCheckpoint().getExecutionOrder());
        } else {
            // Valores por defecto si la alerta no proviene de un escaneo físico directo
            dto.setCheckpointName("N/A");
            dto.setExecutionOrder(0);
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
}
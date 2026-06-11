package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Incident;
import com.siscontrol.backend.dto.IncidentDTO;
import com.siscontrol.backend.repositories.IncidentRepository;
import com.siscontrol.backend.repositories.RoundExecutionRepository;
import com.siscontrol.backend.repositories.ChecklogRepository;
import com.siscontrol.backend.repositories.ShiftRepository;
import com.siscontrol.backend.models.RoundExecution;
import com.siscontrol.backend.models.Checklog;
import com.siscontrol.backend.models.Shift;
import com.siscontrol.backend.enums.IncidentType;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private RoundExecutionRepository roundExecutionRepository;

    @Autowired
    private ChecklogRepository checklogRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private com.siscontrol.backend.repositories.SupervisorGuardRepository supervisorGuardRepository;

    @Autowired
    private AlertService alertService;
    @Transactional
    public IncidentDTO reportarIncidente(IncidentDTO dto) {
        IncidentType tipoFinal;
        try {
            tipoFinal = IncidentType.valueOf(dto.getType().toUpperCase().trim());
        } catch (Exception e) {
            String opciones = java.util.Arrays.toString(IncidentType.values());
            throw new IllegalArgumentException("Tipo de incidente inválido: " + dto.getType() + ". Opciones: " + opciones);
        }

        Incident incidente = null;
        if (dto.getChecklogId() != null) {
            incidente = incidentRepository.findByChecklogId(dto.getChecklogId()).orElse(null);
        }

        if (incidente == null) {
            incidente = new Incident();
        }

        incidente.setTitle(dto.getTitle());
        incidente.setDescription(dto.getDescription());
        incidente.setSeverity(dto.getSeverity());
        incidente.setImageUrl(dto.getImageUrl());
        incidente.setType(tipoFinal);
        incidente.setLatitude(dto.getLatitude());
        incidente.setLongitude(dto.getLongitude());

        if (dto.getClientTimestamp() != null) {
            incidente.setClientTimestamp(dto.getClientTimestamp());
        } else if (incidente.getClientTimestamp() == null) {
            incidente.setClientTimestamp(LocalDateTime.now());
        }

        // Asignamos la ronda o jornada según el tipo
        if (tipoFinal == IncidentType.JORNADA) {
            incidente.setShiftId(dto.getRoundExecutionId());
        } else if (dto.getRoundExecutionId() != null) {
            RoundExecution re = roundExecutionRepository.findById(dto.getRoundExecutionId()).orElse(null);
            incidente.setRoundExecution(re);
        }

        // Forzar aislamiento absoluto del Checklog/Checkpoint
        if (dto.getChecklogId() != null) {
            Checklog cl = checklogRepository.findById(dto.getChecklogId())
                    .orElseThrow(() -> new ResourceNotFoundException("Checklog no encontrado con ID: " + dto.getChecklogId()));
            incidente.setChecklog(cl);
        } else {
            incidente.setChecklog(null);
        }

        // Guarda el incidente llamando de forma segura a AlertService
        Incident i = this.alertService.registrarYDispararAlerta(incidente);

        // Armamos el DTO de salida de forma segura y limpia
        IncidentDTO respuestaDto = new IncidentDTO();
        respuestaDto.setId(i.getId());
        respuestaDto.setTitle(i.getTitle());
        respuestaDto.setDescription(i.getDescription());
        respuestaDto.setSeverity(i.getSeverity());
        respuestaDto.setImageUrl(i.getImageUrl());
        respuestaDto.setType(i.getType() != null ? i.getType().name() : null);
        respuestaDto.setCreatedAt(i.getCreatedAt());
        respuestaDto.setStatus(i.getStatus());

        // Para evitar problemas con la carga Lazy de Hibernate, buscamos los nombres de auditoría mediante contexto limpio
        if (dto.getRoundExecutionId() != null) {
            respuestaDto.setRoundExecutionId(dto.getRoundExecutionId());
            if (tipoFinal == IncidentType.JORNADA) {
                Shift shift = shiftRepository.findById(dto.getRoundExecutionId()).orElse(null);
                if (shift != null) {
                    if (shift.getWorker() != null) {
                        respuestaDto.setUsername(shift.getWorker().getFullName());
                    }
                    if (shift.getInstallation() != null) {
                        respuestaDto.setClientName(shift.getInstallation().getName());
                    }
                    respuestaDto.setStartTime(shift.getEntryTime());
                    respuestaDto.setEndTime(shift.getExitTime());

                    IncidentDTO.RoundExecutionSummaryDTO summary = new IncidentDTO.RoundExecutionSummaryDTO();
                    summary.setId(shift.getId());
                    summary.setStartTime(shift.getEntryTime());
                    summary.setEndTime(shift.getExitTime());
                    respuestaDto.setRoundExecution(summary);
                }
            } else {
                RoundExecution reContext = roundExecutionRepository.findById(dto.getRoundExecutionId()).orElse(null);
                if (reContext != null) {
                    if (reContext.getWorker() != null) {
                        respuestaDto.setUsername(reContext.getWorker().getFullName());
                    }
                    if (reContext.getInstallation() != null) {
                        respuestaDto.setClientName(reContext.getInstallation().getName());
                    }
                    respuestaDto.setStartTime(reContext.getStartTime());
                    respuestaDto.setEndTime(reContext.getEndTime());

                    IncidentDTO.RoundExecutionSummaryDTO summary = new IncidentDTO.RoundExecutionSummaryDTO();
                    summary.setId(reContext.getId());
                    summary.setStartTime(reContext.getStartTime());
                    summary.setEndTime(reContext.getEndTime());
                    respuestaDto.setRoundExecution(summary);
                }
            }
        }

        if (dto.getChecklogId() != null) {
            respuestaDto.setChecklogId(dto.getChecklogId());
            Checklog clContext = checklogRepository.findById(dto.getChecklogId()).orElse(null);
            if (clContext != null && clContext.getCheckpoint() != null) {
                com.siscontrol.backend.models.Checkpoint cp = clContext.getCheckpoint();
                respuestaDto.setCheckpointName(cp.getName());
                respuestaDto.setCheckpointOrder(cp.getExecutionOrder());

                if (cp.getNfcTagCode() != null && respuestaDto.getDescription() != null && !respuestaDto.getDescription().contains("NFC Tag:")) {
                    respuestaDto.setDescription(respuestaDto.getDescription() + "\nNFC Tag: " + cp.getNfcTagCode());
                }
            }
        } else {
            respuestaDto.setCheckpointName("N/A");
            respuestaDto.setCheckpointOrder(0);
        }

        return respuestaDto;
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> obtenerTodos() {
        // CAMBIO CRÍTICO: Se utiliza el Query optimizado con EntityGraph que junta los JOINs
        return incidentRepository.findAllOptimized().stream()
                .map(this::convertirADTO) // Conserva tu conversor original al 100%
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> obtenerPorSupervisor(Long supervisorId) {
        // Obtener guardias del supervisor
        com.siscontrol.backend.models.User supervisor = new com.siscontrol.backend.models.User();
        supervisor.setId(supervisorId);
        
        List<com.siscontrol.backend.models.SupervisorGuard> asignaciones = supervisorGuardRepository.findBySupervisorId(supervisorId);
                
        List<Long> guardIds = asignaciones.stream()
                .map(sg -> sg.getGuard().getId())
                .toList();

        // Si no tiene guardias asignados, no hay incidentes
        if (guardIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Obtener jornadas y rondas de esos guardias (desde el repositorio respectivo)
        List<Shift> jornadas = shiftRepository.findAll().stream()
                .filter(s -> s.getWorker() != null && guardIds.contains(s.getWorker().getId()))
                .toList();
        List<Long> shiftIds = jornadas.stream().map(Shift::getId).toList();

        List<RoundExecution> rondas = roundExecutionRepository.findAll().stream()
                .filter(r -> r.getWorker() != null && guardIds.contains(r.getWorker().getId()))
                .toList();
        List<Long> roundIds = rondas.stream().map(RoundExecution::getId).toList();

        return incidentRepository.findAllOptimized().stream()
                .filter(i -> (i.getShiftId() != null && shiftIds.contains(i.getShiftId())) || 
                             (i.getRoundExecution() != null && roundIds.contains(i.getRoundExecution().getId())))
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentDTO obtenerPorId(Long id) {
        Incident incidente = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente no encontrado con ID: " + id));
        return convertirADTO(incidente);
    }

    // Expone un punto de acceso público del mapper para controladores externos
    public IncidentDTO forzarMapeoDirecto(Incident i) {
        return this.convertirADTO(i);
    }

    // --- METODO DE MAPEÓ CORREGIDO CON CRUCE DE TEXTO COMPLETO ---
    private IncidentDTO convertirADTO(Incident i) {
        IncidentDTO dto = new IncidentDTO();
        dto.setId(i.getId());
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setSeverity(i.getSeverity());
        dto.setImageUrl(i.getImageUrl());
        dto.setType(i.getType() != null ? i.getType().name() : null);
        dto.setCreatedAt(i.getCreatedAt());
        dto.setStatus(i.getStatus());

        // Desglose relacional para evitar nulos y mapear nombres reales a Android
        if (i.getShiftId() != null || i.getType() == IncidentType.JORNADA) {
            Long sId = i.getShiftId();
            if (sId == null && i.getRoundExecution() != null) {
                sId = i.getRoundExecution().getId();
            }
            if (sId != null) {
                dto.setRoundExecutionId(sId);
                Shift shift = shiftRepository.findById(sId).orElse(null);
                if (shift != null) {
                    if (shift.getWorker() != null) {
                        dto.setUsername(shift.getWorker().getFullName());
                    }
                    if (shift.getInstallation() != null) {
                        dto.setClientName(shift.getInstallation().getName());
                    }
                    dto.setStartTime(shift.getEntryTime());
                    dto.setEndTime(shift.getExitTime());

                    IncidentDTO.RoundExecutionSummaryDTO summary = new IncidentDTO.RoundExecutionSummaryDTO();
                    summary.setId(shift.getId());
                    summary.setStartTime(shift.getEntryTime());
                    summary.setEndTime(shift.getExitTime());
                    dto.setRoundExecution(summary);
                }
            }
        } else if (i.getRoundExecution() != null) {
            dto.setRoundExecutionId(i.getRoundExecution().getId());

            if (i.getRoundExecution().getWorker() != null) {
                dto.setUsername(i.getRoundExecution().getWorker().getFullName());
            }
            if (i.getRoundExecution().getInstallation() != null) {
                dto.setClientName(i.getRoundExecution().getInstallation().getClientName());
            }
            dto.setStartTime(i.getRoundExecution().getStartTime());
            dto.setEndTime(i.getRoundExecution().getEndTime());

            IncidentDTO.RoundExecutionSummaryDTO summary = new IncidentDTO.RoundExecutionSummaryDTO();
            summary.setId(i.getRoundExecution().getId());
            summary.setStartTime(i.getRoundExecution().getStartTime());
            summary.setEndTime(i.getRoundExecution().getEndTime());
            dto.setRoundExecution(summary);
        }

        if (i.getChecklog() != null) {
            dto.setChecklogId(i.getChecklog().getId());
            dto.setChecklogImageUrl(i.getChecklog().getImageUrl()); // <- Agrega esto si quieres cruzar las fotos

            if (i.getChecklog().getCheckpoint() != null) {
                com.siscontrol.backend.models.Checkpoint cp = i.getChecklog().getCheckpoint();

                dto.setCheckpointName(cp.getName());
                dto.setCheckpointOrder(cp.getExecutionOrder());

                if (cp.getNfcTagCode() != null && dto.getDescription() != null && !dto.getDescription().contains("NFC Tag:")) {
                    dto.setDescription(dto.getDescription() + "\nNFC Tag: " + cp.getNfcTagCode());
                }
            }
        }

        return dto;
    }
}
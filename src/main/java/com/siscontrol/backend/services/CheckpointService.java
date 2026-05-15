package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.exception.BadRequestException;
import com.siscontrol.backend.exception.ForbiddenException;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.models.Checkpoint;
import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.CheckpointRepository;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckpointService {

    @Autowired
    private CheckpointRepository checkpointRepository;

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Map<String, Object> guardarCheckpoint(Long editorId, Checkpoint checkpoint) {
        validarPermisos(editorId);

        if (checkpoint.getInstallation() == null || checkpoint.getInstallation().getId() == null) {
            throw new BadRequestException("Debe especificar una instalación válida.");
        }

        Installation inst = installationRepository.findById(checkpoint.getInstallation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada."));

        if (checkpointRepository.findByNfcTagCode(checkpoint.getNfcTagCode()).isPresent()) {
            throw new BadRequestException("El Tag NFC '" + checkpoint.getNfcTagCode() + "' ya está asignado.");
        }

        if (checkpointRepository.existsByInstallationIdAndExecutionOrderAndStatus(inst.getId(), checkpoint.getExecutionOrder(), 1)) {
            throw new BadRequestException("La posición #" + checkpoint.getExecutionOrder() + " ya está ocupada.");
        }

        checkpoint.setInstallation(inst);
        checkpoint.setStatus(1);
        checkpoint.setCreatedBy(editorId);

        Checkpoint guardado = checkpointRepository.save(checkpoint);
        System.out.println(">>> SEEDER/API: Checkpoint creado: " + guardado.getName());

        return Map.of("mensaje", "Checkpoint registrado correctamente.", "checkpoint", guardado);
    }

    @Transactional
    public Checkpoint actualizar(Long editorId, Long id, Checkpoint detalles) {
        validarPermisos(editorId);

        Checkpoint cp = checkpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint no encontrado con ID: " + id));

        if (detalles.getExecutionOrder() != null && !cp.getExecutionOrder().equals(detalles.getExecutionOrder())) {
            boolean ocupado = checkpointRepository.existsByInstallationIdAndExecutionOrderAndStatusAndIdNot(
                    cp.getInstallation().getId(), detalles.getExecutionOrder(), 1, id);
            if (ocupado) {
                throw new BadRequestException("La posición #" + detalles.getExecutionOrder() + " ya está ocupada.");
            }
            cp.setExecutionOrder(detalles.getExecutionOrder());
        }

        if (detalles.getNfcTagCode() != null && !cp.getNfcTagCode().equals(detalles.getNfcTagCode())) {
            if (checkpointRepository.findByNfcTagCode(detalles.getNfcTagCode()).isPresent()) {
                throw new BadRequestException("El nuevo código NFC ya está en uso.");
            }
            cp.setNfcTagCode(detalles.getNfcTagCode());
        }

        cp.setName(detalles.getName());
        cp.setLocationDescription(detalles.getLocationDescription());
        cp.setInstruction(detalles.getInstruction());
        cp.setUpdatedBy(editorId);

        System.out.println(">>> API: Checkpoint ID " + id + " actualizado por editor " + editorId);
        return checkpointRepository.save(cp);
    }

    public List<CheckpointDTO> obtenerPorInstalacion(Long installationId) {
        if (!installationRepository.existsById(installationId)) {
            throw new ResourceNotFoundException("La instalación no existe.");
        }

        // Se removió el .filter(c -> c.getStatus() == 1) para retornar todos los checkpoints
        return checkpointRepository.findByInstallationIdOrderByExecutionOrderAsc(installationId).stream()
                .map(c -> new CheckpointDTO(
                        c.getId(),
                        c.getName(),
                        c.getLocationDescription(),
                        c.getNfcTagCode(),
                        c.getInstallation().getId(),
                        c.getExecutionOrder(),
                        c.getInstruction(),
                        c.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Checkpoint alternarEstado(Long editorId, Long id) {
        validarPermisos(editorId);
        Checkpoint cp = checkpointRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No encontrado"));
        cp.setStatus(cp.getStatus() == 1 ? 0 : 1);
        cp.setUpdatedBy(editorId);
        return checkpointRepository.save(cp);
    }

    @Transactional
    public void eliminarLogico(Long editorId, Long id) {
        validarPermisos(editorId);
        Checkpoint cp = checkpointRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No encontrado"));
        cp.setStatus(0);
        cp.setUpdatedBy(editorId);
        checkpointRepository.save(cp);
    }

    private void validarPermisos(Long editorId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));
        if (editor.getRole() == UserRole.GUARD) {
            throw new ForbiddenException("No tienes permisos para realizar esta acción.");
        }
    }
}
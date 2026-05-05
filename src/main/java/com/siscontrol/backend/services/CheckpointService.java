package com.siscontrol.backend.services;

import com.siscontrol.backend.dto.CheckpointDTO;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;
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

    public Map<String, Object> guardarCheckpoint(Checkpoint checkpoint) {
        Installation inst = installationRepository.findById(checkpoint.getInstallation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada."));

        checkpointRepository.findAll().stream()
                .filter(c -> c.getNfcTagCode().equals(checkpoint.getNfcTagCode()))
                .findFirst()
                .ifPresent(existente -> {
                    throw new BadRequestException("El código NFC '" + checkpoint.getNfcTagCode() +
                            "' ya está asignado a: '" + existente.getName() + "'.");
                });

        checkpoint.setInstallation(inst);
        checkpoint.setStatus(UserStatus.ACTIVE);
        return Map.of("mensaje", "Checkpoint registrado.", "checkpoint", checkpointRepository.save(checkpoint));
    }

    public List<CheckpointDTO> obtenerPorInstalacion(Long installationId) {
        if (!installationRepository.existsById(installationId)) {
            throw new ResourceNotFoundException("La instalación no existe.");
        }
        return checkpointRepository.findByInstallationId(installationId).stream()
                .map(c -> new CheckpointDTO(c.getId(), c.getName(), c.getLocationDescription(), c.getNfcTagCode(), c.getInstallation().getId()))
                .collect(Collectors.toList());
    }

    public void eliminarLogico(Long editorId, Long id) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));

        if (editor.getRole() == UserRole.GUARD) throw new ForbiddenException("No tienes permisos");

        Checkpoint cp = checkpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint no encontrado"));

        cp.setStatus(UserStatus.INACTIVE);
        checkpointRepository.save(cp);
    }
}
package com.siscontrol.backend.services;

import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;
import com.siscontrol.backend.exception.*; // Carpeta en singular como confirmaste
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class InstallationService {

    @Autowired private InstallationRepository installationRepository;
    @Autowired private UserRepository userRepository;

    public Map<String, Object> guardarInstalacion(Installation installation) {
        if (installationRepository.findAll().stream().anyMatch(i -> i.getName().equalsIgnoreCase(installation.getName()))) {
            throw new BadRequestException("Nombre de instalación ya existe.");
        }
        installation.setStatus(UserStatus.ACTIVE);
        return Map.of("mensaje", "Instalación creada correctamente.", "instalacion", installationRepository.save(installation));
    }

    public Object obtenerTodas() {
        List<Installation> lista = installationRepository.findAll();

        if (lista.isEmpty()) {
            // Usamos un LinkedHashMap para que el orden de los campos sea estético
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("total", 0);
            respuesta.put("mensaje", "No existen instalaciones registradas en el sistema.");
            return respuesta;
        }

        // Si hay datos, también podemos devolver un objeto envoltorio o la lista directa
        // Para mantener consistencia, podrías devolver la lista directamente:
        return lista;
    }

    public Installation actualizar(Long editorId, Long id, Installation detalles) {
        validarAdminOSupervisor(editorId);
        Installation inst = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada con ID: " + id));

        inst.setName(detalles.getName());
        inst.setLocation(detalles.getLocation());
        inst.setStatus(detalles.getStatus());
        return installationRepository.save(inst);
    }

    public void eliminarLogico(Long editorId, Long id) {
        validarAdminOSupervisor(editorId);
        Installation inst = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada con ID: " + id));

        inst.setStatus(UserStatus.INACTIVE);
        installationRepository.save(inst);
    }

    private void validarAdminOSupervisor(Long editorId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Editor no encontrado"));

        if (editor.getRole() == UserRole.GUARD) {
            throw new ForbiddenException("No tienes permisos de gestión para realizar esta acción.");
        }
    }
}
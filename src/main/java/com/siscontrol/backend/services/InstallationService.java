package com.siscontrol.backend.services;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.exception.ForbiddenException;
import com.siscontrol.backend.exception.ResourceNotFoundException;
import com.siscontrol.backend.models.Installation;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.InstallationRepository;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InstallationService {

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crea una nueva instalación validando permisos de gestión.
     */
    public Map<String, Object> guardarInstalacion(Long editorId, Installation installation) {
        validarAdminOSupervisor(editorId);

        installation.setCreatedBy(editorId);
        installation.setStatus(1); // 1 = Activo por defecto

        Installation guardada = installationRepository.save(installation);
        return Map.of("mensaje", "Instalación creada con éxito", "instalacion", guardada);
    }

    /**
     * Obtiene todas las instalaciones o un mensaje si la lista está vacía.
     */
    public Object obtenerTodas() {
        List<Installation> lista = installationRepository.findAll();
        if (lista.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("total", 0);
            res.put("mensaje", "No existen instalaciones registradas.");
            return res;
        }
        return lista;
    }

    /**
     * Obtiene una instalación por su ID.
     */
    public Installation obtenerPorId(Long id) {
        return installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada con ID: " + id));
    }

    /**
     * Actualiza los datos de una instalación incluyendo coordenadas GPS.
     */
    public Installation actualizar(Long editorId, Long id, Installation detalles) {
        validarAdminOSupervisor(editorId);

        Installation inst = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada"));

        inst.setName(detalles.getName());
        inst.setAddress(detalles.getAddress());
        inst.setClientName(detalles.getClientName());
        inst.setLatitude(detalles.getLatitude());
        inst.setLongitude(detalles.getLongitude());
        inst.setRadiusInMeters(detalles.getRadiusInMeters());
        inst.setStatus(detalles.getStatus());
        inst.setUpdatedBy(editorId);

        return installationRepository.save(inst);
    }

    /**
     * Alternar estado entre 1 (Activo) y 0 (Inactivo).
     */
    public Installation alternarEstado(Long editorId, Long id) {
        validarAdminOSupervisor(editorId);

        Installation inst = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada"));

        inst.setStatus(inst.getStatus() == 1 ? 0 : 1);
        inst.setUpdatedBy(editorId);

        return installationRepository.save(inst);
    }

    /**
     * Realiza un borrado lógico (status 0).
     */
    public void eliminarLogico(Long editorId, Long id) {
        validarAdminOSupervisor(editorId);

        Installation inst = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada"));

        inst.setStatus(0);
        inst.setUpdatedBy(editorId);
        installationRepository.save(inst);
    }

    /**
     * Lógica de Validación Geográfica:
     * Comprueba si las coordenadas del usuario están dentro del radio permitido de la instalación.
     */
    public boolean verificarUbicacion(Long installationId, Double userLat, Double userLon) {
        Installation inst = installationRepository.findById(installationId)
                .orElseThrow(() -> new ResourceNotFoundException("Instalación no encontrada"));

        if (inst.getLatitude() == null || inst.getLongitude() == null) {
            throw new IllegalStateException("La instalación no tiene coordenadas configuradas.");
        }

        double distancia = calcularDistancia(userLat, userLon, inst.getLatitude(), inst.getLongitude());

        // Retorna true si el guardia está dentro del radio (ej. 100 metros)
        return distancia <= inst.getRadiusInMeters();
    }

    /**
     * Fórmula de Haversine para calcular distancia entre dos puntos geográficos (en metros).
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000; // Radio de la Tierra en metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Valida que el usuario que intenta realizar cambios sea ADMIN o SUPERVISOR.
     */
    private void validarAdminOSupervisor(Long editorId) {
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario editor no encontrado"));

        if (editor.getRole() == UserRole.GUARD) {
            throw new ForbiddenException("No tienes permisos de gestión para realizar esta acción.");
        }
    }
}
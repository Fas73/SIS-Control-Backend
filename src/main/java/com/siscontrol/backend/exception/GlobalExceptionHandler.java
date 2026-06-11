package com.siscontrol.backend.exception;

import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Recursos no encontrados (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 2. Errores de lógica de negocio (400)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 3. Permisos denegados / Restricciones de negocio (403)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 4. Parámetros requeridos faltantes en la URL (?param=...)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        String mensaje = String.format("El parámetro obligatorio '%s' de tipo %s no viene en la solicitud.",
                ex.getParameterName(), ex.getParameterType());
        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 5. Tipo de dato incorrecto en parámetros de URL (ej: mandar texto en un ID numérico)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format("El parámetro '%s' recibió un valor no válido ('%s'). Se esperaba un tipo %s.",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 6. JSON mal estructurado o con sintaxis inválida en el @RequestBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // Si el error es por un mal formato de dato (como la fecha o un número)
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
            com.fasterxml.jackson.databind.exc.InvalidFormatException ife = (com.fasterxml.jackson.databind.exc.InvalidFormatException) cause;

            // Caso A: Validar si el fallo fue por un Enum
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String valorIngresado = ife.getValue().toString();
                String opcionesValidas = Arrays.toString(ife.getTargetType().getEnumConstants());
                return buildResponse(HttpStatus.BAD_REQUEST, String.format("El valor '%s' no es válido. Opciones: %s", valorIngresado, opcionesValidas));
            }

            // Caso B: El soplón del campo exacto (Te dirá si falló scannedAt, status, etc.)
            String campoErroneo = ife.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));

            return buildResponse(HttpStatus.BAD_REQUEST, String.format("Error de tipo de dato en el campo '%s'. Valor rechazado: '%s'. Se esperaba: %s",
                    campoErroneo, ife.getValue(), ife.getTargetType().getSimpleName()));
        }

        // Manejo genérico para campos requeridos faltantes basados en excepciones de desempaquetado nativas
        String msg = ex.getLocalizedMessage();
        if (msg != null && msg.contains("instantiation")) {
            Pattern p = Pattern.compile("values\\[\"([^\"]+)\"\\]");
            Matcher m = p.matcher(msg);
            if (m.find()) {
                return buildResponse(HttpStatus.BAD_REQUEST, "Error: El campo obligatorio '" + m.group(1) + "' no puede ser nulo.");
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Error: El cuerpo JSON de la solicitud está mal estructurado o contiene tipos de datos incompatibles generales.");
    }

    // 7. Endpoint / URL no encontrada en el enrutador de Spring (404 estándar)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        String mensaje = "La ruta solicitada '" + ex.getResourcePath() + "' no existe en este servidor.";
        return buildResponse(HttpStatus.NOT_FOUND, mensaje);
    }

    // 8. Corrección Crítica: Manejo inteligente de violaciones de integridad y duplicados en Base de Datos (409 / 400)
    @ExceptionHandler({
            org.springframework.dao.DataAccessException.class,
            org.springframework.dao.DataIntegrityViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleDatabaseDuplicates(Exception ex) {
        String mensaje = "Error de datos: Ocurrió una infracción en las restricciones de la base de datos.";

        // Extraemos el mensaje real de la causa raíz de la base de datos
        String rootCause = "";
        if (ex instanceof org.springframework.dao.DataIntegrityViolationException) {
            org.springframework.dao.DataIntegrityViolationException dive = (org.springframework.dao.DataIntegrityViolationException) ex;
            rootCause = dive.getRootCause() != null ? dive.getRootCause().getMessage().toLowerCase() : "";
        } else {
            rootCause = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        }

        // Caso A: El error se debe al tamaño del string (ej: Base64 de la foto supera el tipo de columna TEXT)
        if (rootCause.contains("too long") || rootCause.contains("truncation") || rootCause.contains("data too long")) {
            mensaje = "Error: El tamaño del archivo o imagen adjunta supera el límite máximo permitido por el servidor.";
            return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
        }

        // Caso B: El error se debe realmente a una restricción única del código del Tag NFC
        if (rootCause.contains("nfc_tag_code") || rootCause.contains("checkpoints") || rootCause.contains("duplicate entry")) {
            mensaje = "El código NFC ya está registrado en el sistema.";
            return buildResponse(HttpStatus.CONFLICT, mensaje);
        }

        // Caso C: Cualquier otro error de integridad referencial o base de datos no mapeado específicamente
        if (!rootCause.isEmpty()) {
            mensaje = "Error de consistencia en base de datos: " + rootCause;
        }

        return buildResponse(HttpStatus.CONFLICT, mensaje);
    }

    // 9. Error General Interno del Servidor (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        String msg = (ex.getMessage() != null && ex.getMessage().contains("null"))
                ? "Error: Faltan datos obligatorios en la solicitud."
                : "Error interno: " + ex.getMessage();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }

    // Metodo utilitario para estandarizar el formato JSON de salida de todos los errores
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}
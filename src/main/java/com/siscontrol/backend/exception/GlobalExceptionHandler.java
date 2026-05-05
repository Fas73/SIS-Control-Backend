package com.siscontrol.backend.exception;

import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

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

    // 3. Permisos denegados (403)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 4. Parámetros faltantes en URL (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    // 5. Método HTTP incorrecto (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " no permitido para esta ruta.");
    }

    // 6. ERROR DE ENUMS (LA SOLUCIÓN A TU PROBLEMA)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonErrors(HttpMessageNotReadableException ex) {
        String mensajeFinal = "Error en el formato del JSON enviado.";
        Throwable cause = ex.getCause();

        // INTENTO A: Usar Jackson directamente (Más preciso)
        if (cause instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String nombreEnum = ife.getTargetType().getSimpleName();
                String opciones = Arrays.toString(ife.getTargetType().getEnumConstants());
                String valorEnviado = ife.getValue().toString();

                mensajeFinal = String.format("Valor '%s' no válido para %s. Favor utilice uno de los existentes: %s",
                        valorEnviado, nombreEnum, opciones);
                return buildResponse(HttpStatus.BAD_REQUEST, mensajeFinal);
            }
        }

        // INTENTO B: Plan B refinado para capturar el valor y las opciones del texto
        String rootMsg = ex.getMessage();
        if (rootMsg != null && rootMsg.contains("not one of the values accepted for Enum class")) {
            try {
                // Esta Regex busca el valor entre comillas y las opciones entre corchetes
                // Ejemplo: "... value "GUARDIA" not one of ... [ADMIN, GUARD, SUPERVISOR]"
                Pattern pValor = Pattern.compile("value \"(.*?)\"");
                Pattern pOpciones = Pattern.compile("\\[(.*?)\\]");
                Pattern pEnum = Pattern.compile("Enum class (?:.*\\.)?(\\w+)");

                Matcher mValor = pValor.matcher(rootMsg);
                Matcher mOpciones = pOpciones.matcher(rootMsg);
                Matcher mEnum = pEnum.matcher(rootMsg);

                String valor = mValor.find() ? mValor.group(1) : "desconocido";
                String opciones = mOpciones.find() ? mOpciones.group(1) : "no disponibles";
                String nombreEnum = mEnum.find() ? mEnum.group(1) : "el campo";

                mensajeFinal = String.format("Valor '%s' no válido para %s. Favor utilice uno de los existentes: [%s]",
                        valor, nombreEnum, opciones);
            } catch (Exception e) {
                mensajeFinal = "Uno de los campos tiene un valor no permitido por el sistema.";
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, mensajeFinal);
    }

    // 7. Tipo de dato erróneo (Ej: letras en un ID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El valor enviado no coincide con el tipo de dato esperado.");
    }

    // 8. Ruta inexistente
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "La ruta /" + ex.getResourcePath() + " no existe.");
    }

    // 9. Error General (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        String msg = (ex.getMessage() != null && ex.getMessage().contains("null"))
                ? "Error: Faltan datos obligatorios en la solicitud."
                : "Error interno: " + ex.getMessage();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}
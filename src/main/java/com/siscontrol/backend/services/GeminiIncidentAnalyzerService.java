package com.siscontrol.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiIncidentAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(GeminiIncidentAnalyzerService.class);

    @Value("${gemini.api.key:}")
    private String apiKeyConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void verificarConfiguracion() {
        String apiKey = getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            log.info("✅ [GeminiAI] API Key cargada correctamente (longitud: {} caracteres).", apiKey.trim().length());
        } else {
            log.warn("⚠️  [GeminiAI] API Key NO configurada. El análisis con IA fallará hasta configurar GEMINI_API_KEY.");
            log.warn("⚠️  [GeminiAI] Define la key en: src/main/resources/application-local.properties");
            log.warn("⚠️  [GeminiAI] Plantilla disponible en: application-local.properties.example");
        }
    }

    public static class IAAnalysisResult {
        public String tipoIncidente;
        public String prioridadIA;
        public String resumenIA;
        public String accionSugeridaIA;
        public boolean requiereAtencionInmediata;
    }

    public IAAnalysisResult analizarIncidente(String title, String type, String severity, String description) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API Key de Gemini no configurada (GEMINI_API_KEY).");
            throw new IllegalStateException("API Key de Gemini no configurada.");
        }

        // Limitar la descripción para evitar exceder tokens
        String descLimitada = description != null ? description : "";
        if (descLimitada.length() > 1000) {
            descLimitada = descLimitada.substring(0, 1000) + "...";
        }

        String prompt = "Actúa como analista operativo de seguridad privada.\n" +
                "Analiza el siguiente incidente o reporte operativo.\n" +
                "Clasifica el incidente y responde ÚNICAMENTE con un objeto JSON válido que siga exactamente esta estructura sin bloques de código ni markdown:\n" +
                "{\n" +
                "  \"tipoIncidente\": \"...\",\n" +
                "  \"prioridadIA\": \"BAJA|MEDIA|ALTA\",\n" +
                "  \"resumenIA\": \"...\",\n" +
                "  \"accionSugeridaIA\": \"...\",\n" +
                "  \"requiereAtencionInmediata\": true|false\n" +
                "}\n\n" +
                "Datos del incidente:\n" +
                "Título: " + (title != null ? title : "Sin título") + "\n" +
                "Tipo Reportado: " + (type != null ? type : "Sin tipo") + "\n" +
                "Gravedad Reportada: " + (severity != null ? severity : "Sin gravedad") + "\n" +
                "Descripción: " + descLimitada + "\n\n" +
                "No inventes datos no presentes. Usa un lenguaje profesional, claro y breve.";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Armar el body según el esquema oficial de Gemini
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(textPart));

            Map<String, Object> content = new HashMap<>();
            content.put("contents", List.of(parts));

            // Forzar respuesta en formato JSON
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            content.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(content, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                JsonNode candidate = root.path("candidates").get(0);
                JsonNode part = candidate.path("content").path("parts").get(0);
                String rawResponse = part.path("text").asText();

                // Limpiar posibles bloques de código de la respuesta
                String cleanedResponse = rawResponse.trim();
                if (cleanedResponse.startsWith("```json")) {
                    cleanedResponse = cleanedResponse.substring(7);
                } else if (cleanedResponse.startsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(3);
                }
                if (cleanedResponse.endsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
                }
                cleanedResponse = cleanedResponse.trim();

                // Parsear a la clase IAAnalysisResult
                JsonNode parsedJson = objectMapper.readTree(cleanedResponse);
                IAAnalysisResult result = new IAAnalysisResult();
                result.tipoIncidente = parsedJson.path("tipoIncidente").asText("OTRO");
                result.prioridadIA = parsedJson.path("prioridadIA").asText("MEDIA");
                result.resumenIA = parsedJson.path("resumenIA").asText("No se pudo generar resumen.");
                result.accionSugeridaIA = parsedJson.path("accionSugeridaIA").asText("Revisar en terreno.");
                result.requiereAtencionInmediata = parsedJson.path("requiereAtencionInmediata").asBoolean(false);

                // Normalizar prioridadIA para asegurar que sea BAJA, MEDIA o ALTA
                String prio = result.prioridadIA.toUpperCase().trim();
                if (!prio.equals("BAJA") && !prio.equals("MEDIA") && !prio.equals("ALTA")) {
                    result.prioridadIA = "MEDIA";
                } else {
                    result.prioridadIA = prio;
                }

                return result;
            } else {
                throw new RuntimeException("Respuesta de API de Gemini no exitosa: " + responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error al comunicarse o parsear la respuesta de Gemini API", e);
            throw new RuntimeException("Falla en la llamada a la IA: " + e.getMessage(), e);
        }
    }

    private String getApiKey() {
        if (apiKeyConfig != null && !apiKeyConfig.trim().isEmpty()) {
            return apiKeyConfig;
        }
        return System.getenv("GEMINI_API_KEY");
    }
}

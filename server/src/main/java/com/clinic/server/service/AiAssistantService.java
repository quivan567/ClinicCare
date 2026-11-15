package com.clinic.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiAssistantService {
    private static final String DISABLED_MESSAGE = "Chưa bật trợ lý AI";

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiAssistantService(@Value("${clinic.ai.enabled:false}") boolean enabled,
                              @Value("${clinic.ai.base-url:}") String baseUrl,
                              @Value("${clinic.ai.api-key:}") String apiKey,
                              @Value("${clinic.ai.model:}") String model,
                              ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isConfigured() {
        return enabled && !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank();
    }

    public String disabledMessage() {
        return DISABLED_MESSAGE;
    }

    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new AiCallException(DISABLED_MESSAGE);
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.putArray("messages")
                    .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                    .add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatCompletionsUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiCallException("Trợ lý AI không phản hồi được");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new AiCallException("Trợ lý AI không trả về nội dung");
            }
            return objectMapper.readTree(extractJson(content));
        } catch (AiCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiCallException("Trợ lý AI không phản hồi được");
        }
    }

    private String chatCompletionsUrl() {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/chat/completions";
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                trimmed = trimmed.substring(firstBreak + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new AiCallException("Trợ lý AI trả về dữ liệu không đọc được");
    }
}

package com.dreamarchive.service;

import com.dreamarchive.dto.dify.DifyWorkflowInput;
import com.dreamarchive.dto.dify.DifyWorkflowOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String runPath;
    private final String apiKey;

    public DifyWorkflowClient(RestTemplate restTemplate,
                              @Value("${dify.base-url}") String baseUrl,
                              @Value("${dify.run-path}") String runPath,
                              @Value("${dify.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.runPath = normalizeRunPath(runPath);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public DifyWorkflowOutput run(DifyWorkflowInput input) {
        if (apiKey.isBlank() || "app-xxxx".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("Dify API key is not configured");
        }

        String url = baseUrl + runPath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("user_id", safe(input.getUserId()));
        inputs.put("dream_post_id", safe(input.getDreamPostId()));
        // Compatibility for workflow variable typo.
        inputs.put("drem_post_id", safe(input.getDreamPostId()));
        inputs.put("dream_room_id", safe(input.getDreamRoomId()));
        inputs.put("dream_room_status", input.getDreamRoomStatus());
        inputs.put("dream_post_content", safe(input.getDreamPostContent()));
        inputs.put("question", safe(input.getQuestion()));

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", safe(input.getUserId()));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null) {
            throw new IllegalStateException("Dify response body is empty");
        }

        Map<String, Object> outputs = extractOutputs(responseBody);
        String answer = toStringValue(outputs.get("answer"));
        boolean isViolation = toBooleanValue(outputs.get("is_violation"));

        if (answer.isBlank()) {
            answer = "Message received. Please try again shortly.";
        }

        return new DifyWorkflowOutput(answer, isViolation);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractOutputs(Map<String, Object> responseBody) {
        Object dataObj = responseBody.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object outputsObj = dataMap.get("outputs");
            if (outputsObj instanceof Map<?, ?> outputMap) {
                return (Map<String, Object>) outputMap;
            }
        }

        Object outputsObj = responseBody.get("outputs");
        if (outputsObj instanceof Map<?, ?> outputMap) {
            return (Map<String, Object>) outputMap;
        }

        log.warn("Dify output format mismatch: {}", responseBody);
        return Map.of();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeRunPath(String path) {
        if (path == null || path.isBlank()) return "/workflows/run";
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toStringValue(Object value) {
        if (value == null) return "";
        return String.valueOf(value);
    }

    private boolean toBooleanValue(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}

package com.dreamarchive.service;

import com.dreamarchive.dto.SentimentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class SentimentClient {

    private static final Logger log = LoggerFactory.getLogger(SentimentClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SentimentClient(RestTemplate restTemplate, @Value("${sentiment.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public Optional<SentimentResponse.SentimentData> analyze(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, String> payload = Map.of("text", text);
            ResponseEntity<SentimentResponse> response =
                    restTemplate.postForEntity(baseUrl + "/analyze", payload, SentimentResponse.class);

            SentimentResponse body = response.getBody();
            if (body != null && body.getCode() != null && body.getCode() == 200 && body.getData() != null) {
                return Optional.of(body.getData());
            }
            log.warn("Sentiment analyze failed: {}", body != null ? body.getMessage() : "empty response");
        } catch (Exception ex) {
            log.warn("Sentiment analyze error: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

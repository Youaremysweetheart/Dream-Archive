package com.dreamarchive.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SentimentResponse {
    private Integer code;
    private String message;
    private SentimentData data;

    @Data
    public static class SentimentData {
        private Integer label;

        @JsonProperty("label_name")
        private String labelName;

        private Double confidence;
        private String intensity;

        @JsonProperty("all_probabilities")
        private Map<String, Double> allProbabilities;

        private String feedback;
    }
}

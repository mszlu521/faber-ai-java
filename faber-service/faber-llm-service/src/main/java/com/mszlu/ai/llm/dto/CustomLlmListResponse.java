package com.mszlu.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CustomLlmListResponse {

    private List<CustomLlmListResponse.CustomLlmItem> llms;
    private Long total;

    @Data
    @Builder
    public static class CustomLlmItem {
        private UUID id;
        private String name;
        private String description;
        private UUID userId;
        private String modelName;
        private UUID providerConfigId;
        private String modelType;
        private Map<String,Object> config;
        private ProviderConfigResponse providerConfig;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}

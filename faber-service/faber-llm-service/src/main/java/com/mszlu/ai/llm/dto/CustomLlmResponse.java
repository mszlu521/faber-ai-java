package com.mszlu.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
@Builder
@Data
public  class CustomLlmResponse {
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
package com.mszlu.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProviderConfigListResponse {

    private List<ProviderConfigItem> providerConfigs;
    private Long total;

    @Data
    @Builder
    public static class ProviderConfigItem{
        private UUID id;
        private UUID userId;
        private String name;
        private String provider;
        private String description;
        private String apiKey;
        private String apiBase;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}

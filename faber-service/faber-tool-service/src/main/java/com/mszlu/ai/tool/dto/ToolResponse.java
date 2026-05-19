package com.mszlu.ai.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResponse {
    private UUID id;
    private UUID creatorId;
    private String name;
    private String description;
    private String toolType;
    private Map<String, Object> parametersSchema;
    private McpConfigResponse mcpConfig;
    private Boolean isEnabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

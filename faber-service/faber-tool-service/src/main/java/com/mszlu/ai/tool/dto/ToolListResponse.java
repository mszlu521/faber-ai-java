package com.mszlu.ai.tool.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ToolListResponse {
    private List<ToolItem> list;
    private Long total;
    private Integer currentPage;
    private Integer pageSize;

    @Data
    public static class ToolItem {
        private UUID id;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime deletedAt;
        private UUID creatorId;
        private String name;
        private String description;
        private String toolType;
        private Boolean isEnable;
        private Map<String, ToolParameterSchema> parametersSchema;
        private Object mcpConfig;
        private Object agents;
    }
}

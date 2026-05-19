package com.mszlu.ai.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ToolBatchAddRequest {
    private List<ToolItem> tools;

    @Data
    public static class ToolItem {
        private String type;
        private UUID id;
    }
}

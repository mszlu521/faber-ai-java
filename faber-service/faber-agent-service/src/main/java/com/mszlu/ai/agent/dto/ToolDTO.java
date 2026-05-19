package com.mszlu.ai.agent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ToolDTO {
    private UUID id;
    private String name;
    private String description;
    private String toolType;
    private String mcpConfig;
}

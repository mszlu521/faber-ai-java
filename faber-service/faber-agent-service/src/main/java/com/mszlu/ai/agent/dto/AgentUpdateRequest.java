package com.mszlu.ai.agent.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AgentUpdateRequest {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private String systemPrompt;
    private String modelProvider;
    private String modelName;
    private Object modelParameters;
    private String openingDialogue;
    private Object suggestedQuestions;
    private Integer version;
    private String status;
    private String visibility;
    private Long invocationCount;
    private OffsetDateTime publishedAt;
    // Agent 模式相关字段
    private String agentMode;
    private Object deepConfig;
}

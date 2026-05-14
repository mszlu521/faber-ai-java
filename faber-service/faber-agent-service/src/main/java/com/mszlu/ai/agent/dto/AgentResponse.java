package com.mszlu.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResponse {

    private UUID id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID creatorId;
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

    // 关联的实体信息
//    private List<ToolDTO> tools;
//    private List<KnowledgeBaseDTO> knowledgeBases;
//    private List<WorkflowDTO> workflows;
//    private List<AgentMarketDTO> agentMarkets;
//    private List<SkillDTO> skills;
}

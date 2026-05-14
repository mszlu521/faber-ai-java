package com.mszlu.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.config.JsonbTypeHandler;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("agents")
public class Agent extends BaseEntity {

    private java.util.UUID creatorId;
    private String name;
    private String description;
    private String icon;
    private String systemPrompt;
    private String modelProvider = "openai";
    private String modelName = "gpt-4";

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String modelParameters;

    private String openingDialogue;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String suggestedQuestions;

    private Integer version = 1;
    private String status = "draft";
    private String visibility = "private";
    private Long invocationCount = 0L;
    private OffsetDateTime publishedAt;

    /**
     * Agent 运行模式：general（通用）、supervisor（监督者）、deep（深度编排）
     */
    private String agentMode = "general";

    /**
     * Deep Mode 配置，JSON 格式存储
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String deepConfig;
}
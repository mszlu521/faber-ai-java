package com.mszlu.ai.llm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.config.JsonbTypeHandler;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.*;

import java.util.UUID;

/**
 * 自定义大语言模型实体
 * 对应数据库表: llms
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("llms")
public class CustomLlm extends BaseEntity {

    /**
     * 用户ID
     */
    private UUID userId;

    /**
     * 模型显示名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 关联的厂商配置ID
     */
    private UUID providerConfigId;

    /**
     * 实际模型标识(如 gpt-4-turbo)
     */
    private String modelName;

    /**
     * 模型类型: chat-对话, embedding-嵌入, vision-视觉
     */
    private String modelType = "chat";

    /**
     * 模型参数配置(JSONB): maxTokens, temperature等
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String config;

    /**
     * 状态: active-启用, inactive-禁用
     */
    private String status = "active";
}

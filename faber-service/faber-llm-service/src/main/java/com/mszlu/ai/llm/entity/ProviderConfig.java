package com.mszlu.ai.llm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 大模型厂商配置实体
 * 对应数据库表: provider_configs
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("provider_configs")
public class ProviderConfig extends BaseEntity {

    /**
     * 用户ID
     */
    private UUID userId;

    /**
     * 配置名称(如: 我的OpenAI)
     */
    private String name;

    /**
     * 厂商标识(openai, ollama, qwen等)
     */
    private String provider;

    /**
     * 描述信息
     */
    private String description;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API地址(Endpoint)
     */
    private String apiBase;

    /**
     * 状态: active-启用, inactive-禁用
     */
    private String status = "active";
}

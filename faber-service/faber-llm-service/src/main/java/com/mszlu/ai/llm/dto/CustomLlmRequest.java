package com.mszlu.ai.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CustomLlmRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "描述不能为空")
    private String description;
    @NotNull(message = "模型标识不能为空")
    private UUID providerConfigId;
    @NotBlank(message = "模型标识不能为空")
    private String modelName;
    private String modelType;
    private Map<String,Object> config;
    private String status;
}

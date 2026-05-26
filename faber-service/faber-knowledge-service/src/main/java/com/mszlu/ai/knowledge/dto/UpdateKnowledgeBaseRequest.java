package com.mszlu.ai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateKnowledgeBaseRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String description;
    @NotBlank(message = "向量模型名称不能为空")
    private String embeddingModelName;
    @NotBlank(message = "向量模型提供者不能为空")
    private String embeddingModelProvider;
    private String chatModelName;
    private String chatModelProvider;
    private List<String> tags;
    //向量维度 不同的向量模型支持不同的维度 这个决定了向量查询是否可以匹配到内容
    @NotNull(message = "向量维度不能为空")
    private Integer vectorDimension;
}

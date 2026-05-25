package com.mszlu.ai.knowledge.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class KnowledgeBaseResponse {
    private UUID id;
    private String name;
    private List<String> tags;
    private String description;
    private String embeddingModelName;
    private String embeddingModelProvider;
    private String chatModelName;
    private String chatModelProvider;
    private Integer embeddingDimension;
    private String storageType;
    private Object storageConfig;
    private Integer documentCount;
    private Long totalSize;
    private Long createdAt;
    private Long updatedAt;
    private UUID creatorId;
}

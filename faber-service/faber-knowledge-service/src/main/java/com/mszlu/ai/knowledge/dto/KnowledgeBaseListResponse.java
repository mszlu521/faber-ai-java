package com.mszlu.ai.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeBaseListResponse {
    private List<KnowledgeBaseResponse> knowledgeBases;
    private Long total;
}

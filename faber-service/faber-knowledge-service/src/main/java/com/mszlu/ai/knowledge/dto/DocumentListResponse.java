package com.mszlu.ai.knowledge.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DocumentListResponse {
    private List<DocumentResponse> items;
    private Long total;
    
    @Data
    public static class DocumentResponse {
        private UUID id;
        private UUID knowledgeBaseId;
        private String name;
        private String fileType;
        private Long size;
        private Integer tokenCount;
        private String storageKey;
        private String fileHash;
        private String status;
        private String errorMessage;
        private Object metaInfo;
        private Boolean enabled;
        private Long createdAt;
        private Long updatedAt;
    }
}

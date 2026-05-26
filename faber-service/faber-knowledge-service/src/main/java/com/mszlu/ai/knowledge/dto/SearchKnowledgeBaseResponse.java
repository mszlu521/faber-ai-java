package com.mszlu.ai.knowledge.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SearchKnowledgeBaseResponse {
    private String query;
    private List<SearchResult> results;
    private Long total;
    private Long took;
    private UUID kbId;
    
    @Data
    public static class SearchResult {
        private UUID id;
        private UUID documentId;
        private String content;
        private Double score;
        private Object metadata;
        private Integer position;
        private DocumentResponse document;  // 对齐 Go: 完整的 Document 对象
        
        /**
         * 完整的文档信息 - 对齐 Go 的 model.Document 结构
         */
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
}

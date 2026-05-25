package com.mszlu.ai.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeBaseListRequest {

    private KnowledgeBaseQueryParam params;

    @Data
    public static class KnowledgeBaseQueryParam {
        private Integer page = 1;
        private Integer size = 10;
        private String search;
    }
}

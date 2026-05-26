package com.mszlu.ai.knowledge.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UploadDocumentResponse {
    private Integer uploaded;
    private Integer failed;
    private List<DocumentInfo> documents;

    @Data
    public static class DocumentInfo {
        private UUID id;
        private String name;
        private String status;
    }
}

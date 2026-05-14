package com.mszlu.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomLLMListRequest {
    private String name;
    private String modelType;
    private String status;
    private Integer page;
    private Integer pageSize;
}

package com.mszlu.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ModelConfigResponse {

    private UUID id;
    private String provider;
    private String name;
    private String modelName;
    private String apiKey;
    private String apiBase;
    private Object parameters;
}

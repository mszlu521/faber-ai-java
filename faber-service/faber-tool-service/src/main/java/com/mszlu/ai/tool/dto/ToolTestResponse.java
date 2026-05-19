package com.mszlu.ai.tool.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ToolTestResponse {
    private Boolean success;
    private String msg;
    private Map<String, Object> data;
}

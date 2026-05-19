package com.mszlu.ai.core.tools.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolMetadata {

    private String name;
    private String description;
    private String sourceType;
    private String beanClassName;
    private String methodName;
    private String parametersSchema;
    private List<ToolParameter> parameters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolParameter {
        private String name;
        private String description;
        private String type;
        private boolean required;
    }
}

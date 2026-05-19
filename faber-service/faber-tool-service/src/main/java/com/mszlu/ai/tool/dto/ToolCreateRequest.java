package com.mszlu.ai.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ToolCreateRequest {
    @NotBlank(message = "工具名不能为空")
    private String name;
    private String description;
    private boolean isEnabled = true;
}

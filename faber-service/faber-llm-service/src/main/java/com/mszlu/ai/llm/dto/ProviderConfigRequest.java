package com.mszlu.ai.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderConfigRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "厂商不能为空")
    private String provider;
    private String description;
    private String apiKey;
    private String apiBase;
    private String status;
}

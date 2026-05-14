package com.mszlu.ai.agent.feign;

import com.mszlu.ai.common.result.Result;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "faber-llm-service", path = "/api/v1/llms")
public interface LlmServiceClient {

    //这里我们实现两个接口，一个是获取模型配置一个是获取提供商配置
    @GetMapping("/{id}/config")
    Result<ModelConfigResponse> getModelConfig(@PathVariable UUID id);

    @GetMapping("/provider-config")
    Result<ProviderConfigResponse> getProviderConfig(
            @RequestParam("provider") String provider,
            @RequestParam("modelName") String modelName
            );

    @Data
    class ModelConfigResponse {
        private UUID id;
        private String provider;
        private String name;
        private String modelName;
        private String apiKey;
        private String apiBase;
        private Object parameters;
    }
    @Data
    class ProviderConfigResponse {
        private String provider;
        private String apiKey;
        private String apiBase;
        private Object parameters;
    }
}

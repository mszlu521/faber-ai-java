package com.mszlu.ai.llm.controller;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.llm.dto.*;
import com.mszlu.ai.llm.service.CustomLlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/llms")
@RequiredArgsConstructor
public class CustomLlmController {

    private final CustomLlmService customLlmService;

    @GetMapping
    public Result<CustomLlmListResponse> listLLMs(
            @RequestParam(value = "modelType", required = false) String modelType,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int pageSize
    ) {
        CustomLLMListRequest request = CustomLLMListRequest
                .builder()
                .modelType(modelType)
                .name(name)
                .page(page)
                .pageSize(pageSize)
                .build();
        CustomLlmListResponse response = customLlmService.listLLMs(request);
        return Result.success(response);
    }

    @PostMapping
    public Result<CustomLlmResponse> createLLM(@Valid  @RequestBody CustomLlmRequest request) {
        CustomLlmResponse response = customLlmService.createLLM(request);
        return Result.success(response);
    }
    @GetMapping("/all")
    public Result<List<CustomLlmResponse>> listAllLlm() {
        List<CustomLlmResponse> response = customLlmService.listAllLlm();
        return Result.success(response);
    }
    @DeleteMapping("/{id}")
    public Result<Void> deleteLLM(@PathVariable UUID id) {
        customLlmService.deleteLLM(id);
        return Result.success();
    }

    @GetMapping("/{id}/config")
    public Result<ModelConfigResponse> getLLMConfig(@PathVariable UUID id) {
        ModelConfigResponse response = customLlmService.getLLMConfig(id);
        return Result.success(response);
    }
    @GetMapping("/provider-config")
    public Result<ProviderConfigAgentResponse> getProviderConfig(
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "modelName", required = false) String modelName
    ){
        ProviderConfigAgentResponse response = customLlmService.getProviderConfig(provider, modelName);
        return Result.success(response);
    }
}

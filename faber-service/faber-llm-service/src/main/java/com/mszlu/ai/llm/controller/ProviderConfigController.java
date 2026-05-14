package com.mszlu.ai.llm.controller;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.llm.dto.ProviderConfigListResponse;
import com.mszlu.ai.llm.dto.ProviderConfigRequest;
import com.mszlu.ai.llm.dto.ProviderConfigResponse;
import com.mszlu.ai.llm.service.ProviderConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider-configs")
@RequiredArgsConstructor
public class ProviderConfigController {

    private final ProviderConfigService providerConfigService;

    @GetMapping
    public Result<ProviderConfigListResponse> listConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ){
        ProviderConfigListResponse providerConfigListResponse = providerConfigService.listConfigs(page,size);
        return Result.success(providerConfigListResponse);
    }
    @PostMapping
    public Result<ProviderConfigResponse> createConfig(@Valid @RequestBody ProviderConfigRequest request){
        ProviderConfigResponse providerConfigResponse = providerConfigService.createConfig(request);
        return Result.success(providerConfigResponse);
    }
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable UUID id){
        providerConfigService.deleteConfig(id);
        return Result.success();
    }
}

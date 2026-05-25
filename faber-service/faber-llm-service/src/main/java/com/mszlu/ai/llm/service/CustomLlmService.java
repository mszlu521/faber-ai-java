package com.mszlu.ai.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.llm.dto.*;
import com.mszlu.ai.llm.entity.CustomLlm;
import com.mszlu.ai.llm.entity.ProviderConfig;
import com.mszlu.ai.llm.mapper.CustomLlmMapper;
import com.mszlu.ai.llm.mapper.ProviderConfigMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomLlmService {
    private final CustomLlmMapper customLlmMapper;
    private final ProviderConfigMapper providerConfigMapper;
    private final ObjectMapper objectMapper;
    private final CommonService commonService;
    public CustomLlmListResponse listLLMs(CustomLLMListRequest request) {
        UUID userId = UserContext.getUserId();
        Page<CustomLlm> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getUserId, userId)
                .like(StringUtils.isNotBlank(request.getName()), CustomLlm::getName, request.getName())
                .eq(StringUtils.isNotBlank(request.getModelType()),CustomLlm::getModelType, request.getModelType())
                .eq(StringUtils.isNotBlank(request.getStatus()),CustomLlm::getStatus, request.getStatus());
        Page<CustomLlm> customLlmPage = this.customLlmMapper
                .selectPage(page,queryWrapper);
        List<CustomLlmListResponse.CustomLlmItem> customLlmItems = toCustomLlmItems(customLlmPage.getRecords());
        return CustomLlmListResponse.builder()
                .llms(customLlmItems)
                .total(customLlmPage.getTotal())
                .build();
    }

    private List<CustomLlmListResponse.CustomLlmItem> toCustomLlmItems(List<CustomLlm> records) {
        return records.stream().map(this::toCustomLlmItem).collect(Collectors.toList());
    }
    private CustomLlmListResponse.CustomLlmItem toCustomLlmItem(CustomLlm llm) {
        CustomLlmListResponse.CustomLlmItem item = CustomLlmListResponse.CustomLlmItem.builder()
                .id(llm.getId())
                .name(llm.getName())
                .description(llm.getDescription())
                .userId(llm.getUserId())
                .modelName(llm.getModelName())
                .providerConfigId(llm.getProviderConfigId())
                .status(llm.getStatus())
                .modelType(llm.getModelType()).build();
        ProviderConfig providerConfig = this.providerConfigMapper.selectById(llm.getProviderConfigId());
        if (providerConfig == null){
            throw new BusinessException("ProviderConfig not found");
        }
        item.setProviderConfig(toProviderConfigResponse(providerConfig));
        if (llm.getConfig() != null){
            try {
                item.setConfig(objectMapper.readValue(llm.getConfig(), Map.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return item;
    }

    private ProviderConfigResponse toProviderConfigResponse(ProviderConfig providerConfig) {
        return ProviderConfigResponse.builder()
                .id(providerConfig.getId())
                .userId(providerConfig.getUserId())
                .name(providerConfig.getName())
                .provider(providerConfig.getProvider())
                .description(providerConfig.getDescription())
                .apiKey(providerConfig.getApiKey())
                .apiBase(providerConfig.getApiBase())
                .status(providerConfig.getStatus())
                .createdAt(providerConfig.getCreatedAt())
                .updatedAt(providerConfig.getUpdatedAt()).build();
    }

    public CustomLlmResponse createLLM(@Valid CustomLlmRequest request) {
        UUID userId = UserContext.getUserId();
        CustomLlm customLlm = new CustomLlm();
        customLlm.setUserId(userId);
        customLlm.setName(request.getName());
        customLlm.setDescription(request.getDescription());
        customLlm.setModelName(request.getModelName());
        customLlm.setProviderConfigId(request.getProviderConfigId());
        customLlm.setModelType(request.getModelType());
        customLlm.setStatus(request.getStatus());
        if (request.getConfig() != null){
            try {
                customLlm.setConfig(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        this.customLlmMapper.insert(customLlm);
        return toResponse(customLlm);
    }

    private CustomLlmResponse toResponse(CustomLlm customLlm) {
        Map<String,Object> config = null;
        if (customLlm.getConfig() != null){
            try {
                config = objectMapper.readValue(customLlm.getConfig(), Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        ProviderConfig providerConfig = this.providerConfigMapper.selectById(customLlm.getProviderConfigId());
        return CustomLlmResponse.builder()
                .id(customLlm.getId())
                .name(customLlm.getName())
                .description(customLlm.getDescription())
                .userId(customLlm.getUserId())
                .modelName(customLlm.getModelName())
                .providerConfigId(customLlm.getProviderConfigId())
                .modelType(customLlm.getModelType())
                .status(customLlm.getStatus())
                .config(config)
                .providerConfig(toProviderConfigResponse(providerConfig)).build();
    }

    public List<CustomLlmResponse> listAllLlm(String modelType) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getUserId, userId);
        queryWrapper.eq(StringUtils.isNotBlank(modelType),CustomLlm::getModelType, modelType);
        List<CustomLlm> customLlms = this.customLlmMapper.selectList(queryWrapper);
        return customLlms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void deleteLLM(UUID id) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getUserId, userId)
                .eq(CustomLlm::getId, id);
        if (!this.customLlmMapper.exists(queryWrapper)){
            throw new BusinessException("CustomLlm not found");
        }
        this.customLlmMapper.deleteById(id);
    }

    public ModelConfigResponse getLLMConfig(UUID id) {
        UUID userId = UserContext.getUserId();
        CustomLlm customLlm = this.commonService.findLLmByUserIdAndId(userId, id);
        if (customLlm == null){
            throw new BusinessException("CustomLlm not found");
        }
        //查询厂商配置
        ProviderConfig providerConfig = this.providerConfigMapper.selectById(customLlm.getProviderConfigId());
        if (providerConfig == null){
            throw new BusinessException("ProviderConfig not found");
        }
        Map<String,Object> config = null;
        if (customLlm.getConfig() != null){
            try {
                config = objectMapper.readValue(customLlm.getConfig(), Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return ModelConfigResponse.builder()
                .id(customLlm.getId())
                .provider(providerConfig.getProvider())
                .name(customLlm.getName())
                .modelName(customLlm.getModelName())
                .apiKey(providerConfig.getApiKey())
                .apiBase(providerConfig.getApiBase())
                .parameters(config)
                .build();
    }

    public ProviderConfigAgentResponse getProviderConfig(String provider, String modelName) {
        UUID userId = UserContext.getUserId();
        ProviderConfig providerConfig = commonService.findProvideConfigByNameAndUserId(provider,userId);
        if (providerConfig == null){
            throw new BusinessException("ProviderConfig not found");
        }
        CustomLlm customLlm = commonService.findLLmByProviderIdAndModelName(providerConfig.getId(), modelName);
        return toProviderConfigAgentResponse(providerConfig,customLlm);
    }

    private ProviderConfigAgentResponse toProviderConfigAgentResponse(ProviderConfig providerConfig, CustomLlm customLlm) {
        Map<String,Object> config = null;
        if (customLlm.getConfig() != null){
            try {
                config = objectMapper.readValue(customLlm.getConfig(), Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return ProviderConfigAgentResponse.builder()
                .id(providerConfig.getId())
                .name(providerConfig.getName())
                .provider(providerConfig.getProvider())
                .description(providerConfig.getDescription())
                .apiKey(providerConfig.getApiKey())
                .apiBase(providerConfig.getApiBase())
                .status(providerConfig.getStatus())
                .parameters(config)
                .build();
    }
}

package com.mszlu.ai.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.llm.dto.ProviderConfigListResponse;
import com.mszlu.ai.llm.dto.ProviderConfigRequest;
import com.mszlu.ai.llm.dto.ProviderConfigResponse;
import com.mszlu.ai.llm.entity.CustomLlm;
import com.mszlu.ai.llm.entity.ProviderConfig;
import com.mszlu.ai.llm.mapper.CustomLlmMapper;
import com.mszlu.ai.llm.mapper.ProviderConfigMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mszlu.ai.common.result.ResultCode.PROVIDER_NOT_DELETE;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderConfigService {
    private final ProviderConfigMapper providerConfigMapper;
    private final CustomLlmMapper customLlmMapper;
    public ProviderConfigListResponse listConfigs(int page, int size) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<ProviderConfig> queryWrapper = new LambdaQueryWrapper<>();
        Page<ProviderConfig> pageParam = new Page<>(page, size);
        queryWrapper.eq(ProviderConfig::getUserId, userId)
                .orderByDesc(ProviderConfig::getCreatedAt);
        Page<ProviderConfig> providerConfigPage = providerConfigMapper.selectPage(pageParam, queryWrapper);
        List<ProviderConfig> records = providerConfigPage.getRecords();
        List<ProviderConfigListResponse.ProviderConfigItem> providerConfigItems = toConfigItem(records);
        return ProviderConfigListResponse.builder()
                .providerConfigs(providerConfigItems)
                .total(providerConfigPage.getTotal())
                .build();
    }

    private List<ProviderConfigListResponse.ProviderConfigItem> toConfigItem(List<ProviderConfig> records) {
        return records.stream().map(record -> ProviderConfigListResponse.ProviderConfigItem.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .name(record.getName())
                .provider(record.getProvider())
                .description(record.getDescription())
                .apiKey(record.getApiKey())
                .apiBase(record.getApiBase())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    public ProviderConfigResponse createConfig(@Valid ProviderConfigRequest request) {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setUserId(UserContext.getUserId());
        providerConfig.setName(request.getName());
        providerConfig.setProvider(request.getProvider());
        providerConfig.setDescription(request.getDescription());
        providerConfig.setApiKey(request.getApiKey());
        providerConfig.setApiBase(request.getApiBase());
        providerConfig.setStatus(request.getStatus());
        providerConfigMapper.insert(providerConfig);
        return toResponse(providerConfig);
    }

    private ProviderConfigResponse toResponse(ProviderConfig providerConfig) {
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

    public void deleteConfig(UUID id) {
        //判断如果厂商下有模型 提示不能删除
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getProviderConfigId, id)
                .eq(CustomLlm::getUserId, userId);
        if (this.customLlmMapper.exists(queryWrapper)){
            throw new BusinessException(PROVIDER_NOT_DELETE);
        }
        this.providerConfigMapper.deleteById(id);
    }
}

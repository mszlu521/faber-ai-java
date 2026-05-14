package com.mszlu.ai.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mszlu.ai.llm.entity.CustomLlm;
import com.mszlu.ai.llm.entity.ProviderConfig;
import com.mszlu.ai.llm.mapper.CustomLlmMapper;
import com.mszlu.ai.llm.mapper.ProviderConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {
    private final CustomLlmMapper customLlmMapper;
    private final ProviderConfigMapper providerConfigMapper;

    public CustomLlm findLLmByUserIdAndId(UUID userId, UUID id) {
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getUserId, userId).eq(CustomLlm::getId, id);
        return this.customLlmMapper.selectOne(queryWrapper);
    }

    public ProviderConfig findProvideConfigByNameAndUserId(String name, UUID userId) {
        LambdaQueryWrapper<ProviderConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProviderConfig::getName, name).eq(ProviderConfig::getUserId, userId);
        return this.providerConfigMapper.selectOne(queryWrapper);
    }

    public CustomLlm findLLmByProviderIdAndModelName(UUID providerConfigId, String modelName) {
        LambdaQueryWrapper<CustomLlm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomLlm::getProviderConfigId, providerConfigId)
                .eq(CustomLlm::getModelName, modelName);
        return this.customLlmMapper.selectOne(queryWrapper);
    }
}

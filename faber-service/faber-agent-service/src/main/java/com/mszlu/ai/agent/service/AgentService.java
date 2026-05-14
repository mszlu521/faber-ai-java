package com.mszlu.ai.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.agent.dto.*;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.agent.mapper.AgentMapper;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.result.ResultCode;
import com.mszlu.ai.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {
    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper;
    public AgentListResponse listAgents(AgentQueryRequest request) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getCreatorId, userId)
                .like(StringUtils.isNotBlank(request.getName()),
                        Agent::getName, request.getName())
                .orderByDesc(Agent::getCreatedAt);
        Page<Agent> page = new Page<>(request.getPage(), request.getPageSize());
        Page<Agent> agentPage = agentMapper.selectPage(page, queryWrapper);
        AgentListResponse response = new AgentListResponse();
        response.setAgents(agentPage.getRecords()
                .stream()
                .map(this::toResponse)
                .toList()
        );
        response.setTotal(agentPage.getTotal());
        return response;
    }

    private AgentResponse toResponse(Agent agent) {
        Object modelParams = null;
        if (agent.getModelParameters() != null){
            try {
                modelParams = objectMapper.readValue(agent.getModelParameters(), Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        Object suggestedQuestions = null;
        if (agent.getSuggestedQuestions() != null){
            try {
                suggestedQuestions = objectMapper.readValue(agent.getSuggestedQuestions(), Object.class);
            }catch (JsonProcessingException e){
                throw new RuntimeException(e);
            }
        }
        Object deepConfig = null;
        if (agent.getDeepConfig() != null){
            try {
                deepConfig = objectMapper.readValue(agent.getDeepConfig(), Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return AgentResponse.builder()
                .id(agent.getId())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .creatorId(agent.getCreatorId())
                .name(agent.getName())
                .description(agent.getDescription())
                .openingDialogue(agent.getOpeningDialogue())
                .suggestedQuestions(suggestedQuestions)
                .modelParameters(modelParams)
                .version(agent.getVersion())
                .status(agent.getStatus())
                .visibility(agent.getVisibility())
                .invocationCount(agent.getInvocationCount())
                .publishedAt(agent.getPublishedAt())
                .agentMode(agent.getAgentMode())
                .deepConfig(deepConfig)
                .icon(agent.getIcon())
                .systemPrompt(agent.getSystemPrompt())
                .modelProvider(agent.getModelProvider())
                .modelName(agent.getModelName()).build();
    }

    public AgentResponse createAgent(AgentCreateRequest request) {
        UUID userId = UserContext.getUserId();
        Agent agent = new Agent();
        agent.setCreatorId(userId);
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        agent.setSystemPrompt("");
        agent.setVersion(1);
        agent.setInvocationCount(0L);
        agent.setVisibility("private");
        agent.setAgentMode(request.getAgentMode() != null ? request.getAgentMode() : "general");
        if (request.getDeepConfig() != null){
            try {
                agent.setDeepConfig(objectMapper.writeValueAsString(request.getDeepConfig()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
        }
        agentMapper.insert(agent);
        return toResponse(agent);
    }

    public AgentResponse getAgent(UUID id) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, id)
                .eq(Agent::getCreatorId, userId);
        Agent agent = agentMapper.selectOne(queryWrapper);
        if (agent == null){
            throw new BusinessException(ResultCode.AGENT_NOT_FOUND);
        }
        //这里注意后续我们关联工具等信息时，这里需要都关联查询出来
        return toResponse(agent);
    }

    public AgentResponse updateAgent(AgentUpdateRequest request) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, request.getId())
                .eq(Agent::getCreatorId, userId);
        Agent agent = agentMapper.selectOne(queryWrapper);
        if (agent == null){
            throw new BusinessException(ResultCode.AGENT_NOT_FOUND);
        }
        if (request.getName() != null){
            agent.setName(request.getName());
        }
        if (request.getDescription() != null){
            agent.setDescription(request.getDescription());
        }
        if (request.getStatus() != null){
            agent.setStatus(request.getStatus());
        }
        if (request.getAgentMode() != null){
            agent.setAgentMode(request.getAgentMode());
        }
        if (request.getIcon() != null){
            agent.setIcon(request.getIcon());
        }
        if (request.getSystemPrompt() != null){
            agent.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getModelProvider() != null){
            agent.setModelProvider(request.getModelProvider());
        }
        if (request.getModelName() != null){
            agent.setModelName(request.getModelName());
        }
        if (request.getModelParameters() != null){
            try {
                agent.setModelParameters(objectMapper.writeValueAsString(request.getModelParameters()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (request.getDeepConfig() != null){
            try {
                agent.setDeepConfig(objectMapper.writeValueAsString(request.getDeepConfig()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        agent.setUpdatedAt(OffsetDateTime.now());
        agent.setVersion(agent.getVersion() + 1);
        agent.setOpeningDialogue(request.getOpeningDialogue());
        agentMapper.updateById(agent);
        return toResponse(agent);
    }

    public void deleteAgent(UUID id) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, id)
                .eq(Agent::getCreatorId, userId);
        agentMapper.delete(queryWrapper);
    }
}

package com.mszlu.ai.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mszlu.ai.agent.dto.ToolDTO;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.agent.entity.AgentTool;
import com.mszlu.ai.agent.feign.ToolServiceClient;
import com.mszlu.ai.agent.mapper.AgentMapper;
import com.mszlu.ai.agent.mapper.AgentToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {

    private final AgentMapper agentMapper;
    private final AgentToolMapper agentToolMapper;
    private final ToolServiceClient toolServiceClient;

    public Agent getAgentById(UUID userId, UUID agentId) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, agentId);
        queryWrapper.eq(Agent::getCreatorId, userId);
        return agentMapper.selectOne(queryWrapper);
    }

    public List<ToolDTO> getAgentTools(UUID agentId) {
        LambdaQueryWrapper<AgentTool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentTool::getAgentId, agentId);
        List<AgentTool> tools = agentToolMapper.selectList(queryWrapper);
        return tools.stream().map(
                tool -> toolServiceClient.getTool(tool.getToolId()).getData())
                .toList();
    }
}

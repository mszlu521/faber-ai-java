package com.mszlu.ai.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.agent.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {

    private final AgentMapper agentMapper;

    public Agent getAgentById(UUID userId, UUID agentId) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, agentId);
        queryWrapper.eq(Agent::getCreatorId, userId);
        return agentMapper.selectOne(queryWrapper);
    }
}

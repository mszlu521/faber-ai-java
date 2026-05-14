package com.mszlu.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mszlu.ai.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}

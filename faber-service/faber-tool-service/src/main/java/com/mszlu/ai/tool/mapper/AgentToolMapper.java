package com.mszlu.ai.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mszlu.ai.tool.entity.AgentTool;
import com.mszlu.ai.tool.entity.Tool;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentToolMapper extends BaseMapper<AgentTool> {
}

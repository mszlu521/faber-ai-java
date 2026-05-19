package com.mszlu.ai.tool.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@TableName("agent_tools")
public class AgentTool {

    private UUID agentId;
    private UUID toolId;
    private String status = "active";
    private OffsetDateTime createdAt;

    @Getter
    @Setter
    public static class AgentToolId implements Serializable {
        private UUID agentId;
        private UUID toolId;

        public AgentToolId() {}

        public AgentToolId(UUID agentId, UUID toolId) {
            this.agentId = agentId;
            this.toolId = toolId;
        }
    }
}
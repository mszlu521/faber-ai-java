package com.mszlu.ai.agent.dto;

import lombok.Data;

@Data
public class AgentCreateRequest {
    private String name;
    private String description;
    private String status;
    private String agentMode;
    private Object deepConfig;
}

package com.mszlu.ai.agent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AgentMessageRequest {

    private UUID agentId;
    private String message;
    private UUID sessionId;
}

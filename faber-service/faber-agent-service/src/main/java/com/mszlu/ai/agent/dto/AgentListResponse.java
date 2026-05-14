package com.mszlu.ai.agent.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentListResponse {
    private List<AgentResponse> agents;
    private Long total;
}

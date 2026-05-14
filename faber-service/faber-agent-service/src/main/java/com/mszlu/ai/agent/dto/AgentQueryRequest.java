package com.mszlu.ai.agent.dto;

import lombok.Data;

@Data
public class AgentQueryRequest {

    private String name;
    private String status;
    private Integer page = 1;
    private Integer pageSize = 10;
}

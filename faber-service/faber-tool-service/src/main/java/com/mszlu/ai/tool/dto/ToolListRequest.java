package com.mszlu.ai.tool.dto;

import lombok.Data;

@Data
public class ToolListRequest {

    private String name;
    private String type;
    private Integer page = 1;
    private Integer pageSize = 10;
}

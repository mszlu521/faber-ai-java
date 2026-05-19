package com.mszlu.ai.tool.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ToolParameterSchema {
    private String Type;
    private String ElemInfo;
    private Map<String, ToolParameterSchema> SubParams;
    private String Desc;
    private List<String> Enum;
    private Boolean Required;
}
package com.mszlu.ai.tool.dto;

import lombok.Data;

@Data
public class McpConfigRequest {
    
    private String type; // sse, http, stdio
    
    private String url;
    
    private Boolean authenticationRequired;
    
    private String credentialType;

    private String endpoint;
}
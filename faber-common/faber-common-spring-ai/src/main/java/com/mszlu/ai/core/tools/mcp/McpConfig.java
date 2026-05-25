package com.mszlu.ai.core.tools.mcp;

import lombok.Data;

@Data
public class McpConfig {

    private String type; // sse, http, stdio

    private String url;

    private Boolean authenticationRequired;

    private String credentialType;

    private String endpoint;
}

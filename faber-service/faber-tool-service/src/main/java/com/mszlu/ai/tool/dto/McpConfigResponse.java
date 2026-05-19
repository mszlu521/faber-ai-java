
package com.mszlu.ai.tool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpConfigResponse {
    
    private String type;
    private String url;
    private Boolean authenticationRequired;
    private String credentialType;
}

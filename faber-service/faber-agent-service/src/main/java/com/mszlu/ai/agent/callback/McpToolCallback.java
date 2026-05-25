package com.mszlu.ai.agent.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.core.tools.mcp.McpClientService;
import com.mszlu.ai.core.tools.mcp.McpConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

@Slf4j
public class McpToolCallback implements ToolCallback {
    private McpSchema.Tool mcpTool;
    private McpConfig mcpConfig;
    private McpClientService mcpClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public McpToolCallback(McpSchema.Tool mcpTool, McpConfig mcpConfig, McpClientService mcpClientService) {
        this.mcpTool = mcpTool;
        this.mcpConfig = mcpConfig;
        this.mcpClientService = mcpClientService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(mcpTool.name())
                .description(mcpTool.description())
                .inputSchema(convertInputSchema())
                .build();
    }

    private String convertInputSchema() {
        if (mcpTool.inputSchema() != null) {
            try {
                return objectMapper.writeValueAsString(mcpTool.inputSchema());
            } catch (JsonProcessingException e) {
                log.error("Failed to convert input schema to JSON", e);
                return "{}";
            }
        }
        return "{}";
    }

    @Override
    public String call(String toolInput) {
        McpSyncClient mcpClient = null ;
        try {
            mcpClient = mcpClientService.getClient(mcpConfig);
            Map<String, Object> arguments = objectMapper.readValue(toolInput, Map.class);
            //调用mcp工具
            McpSchema.CallToolResult result = mcpClient.callTool(new McpSchema.CallToolRequest(mcpTool.name(), arguments));
            //转换结果为json字符串
            if (result != null && result.content() != null){
                return objectMapper.writeValueAsString(result.content());
            }else{
                return "{}";
            }
        }catch (Exception e){
            log.error("Failed to call tool", e);
            return "{}";
        }finally {
            if (mcpClient != null) {
                try {
                    mcpClient.closeGracefully();
                } catch (Exception e) {
                   log.error("Failed to close mcpClient", e);
                }
            }
        }
    }
}

package com.mszlu.ai.core.tools.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {

    private final McpClientManger manager;
    public McpSyncClient getClient(McpConfig mcpConfig) {
        return manager.getClient(mcpConfig);
    }

    public List<McpSchema.Tool> fetchToolsFromMcpServer(McpConfig mcpConfig){
        if (mcpConfig == null){
            return Collections.emptyList();
        }
        McpSyncClient client = getClient(mcpConfig);
        McpSchema.ListToolsResult listToolsResult = client.listTools();
        if (listToolsResult == null || listToolsResult.tools() == null){
            return Collections.emptyList();
        }
        return listToolsResult.tools();
    }
}

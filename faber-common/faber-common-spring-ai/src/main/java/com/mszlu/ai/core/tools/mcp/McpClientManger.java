package com.mszlu.ai.core.tools.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientManger {

    private final Map<String, McpClientHolder> cache = new ConcurrentHashMap<>();

    public McpSyncClient getClient(McpConfig mcpConfig) {
        String cacheKey = mcpConfig.getUrl() + mcpConfig.getEndpoint();
        McpClientHolder holder = cache.compute(cacheKey, (key, existing) -> {
            if (existing != null && !existing.isBroken()){
                return existing;
            }
            //如果之前有失败的client 先清理资源
            if (existing != null && existing.isBroken()) {
                try {
                    existing.client.close();
                }catch (Exception e){
                    //忽略关闭失败的资源
                }
            }
            return createHolder(mcpConfig);
        });
        if (holder.isBroken()){
            cache.remove(cacheKey);
        }
        return holder.getClient();
    }

    private McpClientHolder createHolder(McpConfig mcpConfig) {
        McpClientTransport transport;
        boolean isSSE = mcpConfig.getType().equals("sse");
        if (isSSE){
            transport = HttpClientSseClientTransport.builder(mcpConfig.getUrl())
                    .sseEndpoint(mcpConfig.getEndpoint())
                    .customizeRequest(request -> request.header("Authorization", "Bearer " + mcpConfig.getCredentialType()))
                    .build();
        }else{
            transport = HttpClientStreamableHttpTransport.builder(mcpConfig.getUrl())
                    .endpoint(mcpConfig.getEndpoint())
                    .customizeRequest(request -> request.header("Authorization", "Bearer " + mcpConfig.getCredentialType()))
                    .build();
        }
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build()
                )
                .build();
        try {
            //初始化
            client.initialize();
            return new McpClientHolder(client, true);
        }catch (Exception e){
            try {
                client.close();
            }catch (Exception e1){
                //忽略关闭失败的资源
            }
        }
        return new McpClientHolder(client, false);
    }

    @PreDestroy
    public void destroy() {
        //应用关闭时清理有缓存的client
        for (McpClientHolder holder : cache.values()) {
            try {
                holder.client.close();
            }catch (Exception e){
                //忽略关闭失败的资源
            }
        }
        cache.clear();
    }
    static  class McpClientHolder {
        @Getter
        private final McpSyncClient client;
        private final boolean initialized;
        McpClientHolder(McpSyncClient client, boolean initialized) {
            this.client = client;
            this.initialized = initialized;
        }
        public boolean isBroken() {
            return !initialized;
        }
    }
}

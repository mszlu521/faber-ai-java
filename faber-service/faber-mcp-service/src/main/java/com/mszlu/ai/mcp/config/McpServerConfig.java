package com.mszlu.ai.mcp.config;

import com.mszlu.ai.mcp.tools.BasicTools;
import com.mszlu.ai.mcp.tools.WeatherTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {


    @Bean
    public ToolCallbackProvider basicToolsProvider(BasicTools basicTools){
        return MethodToolCallbackProvider.builder()
                .toolObjects(basicTools)
                .build();
    }
    @Bean
    public ToolCallbackProvider weatherToolsProvider(WeatherTools weatherTools){
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)
                .build();
    }
}

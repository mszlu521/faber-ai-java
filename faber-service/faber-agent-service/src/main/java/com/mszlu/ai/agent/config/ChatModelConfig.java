package com.mszlu.ai.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class ChatModelConfig {
    //chatmodel配置工厂 根据不同的模型采用不同的实现


    public ChatModel createModel(String provider,
                                 String modelName,
                                 String baseUrl,
                                 String apiKey,
                                 Double temperature,
                                 Integer maxTokens
    ){
        return switch (provider.toLowerCase()){
            case "dashscope","qwen" ->  createDashScopeModel(baseUrl,modelName,apiKey,temperature,maxTokens);
            default -> createOpenAIModel(baseUrl,modelName,apiKey,temperature,maxTokens);
        };
    }

    private ChatModel createDashScopeModel(String baseUrl,
                                           String modelName,
                                           String apiKey,
                                           Double temperature,
                                           Integer maxTokens) {
        DashScopeApi api = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .maxToken(maxTokens)
                .enableThinking(true)
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions( options)
                .build();
    }

    private ChatModel createOpenAIModel(String baseUrl,
                                        String modelName,
                                        String apiKey,
                                        Double temperature,
                                        Integer maxTokens) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }
}

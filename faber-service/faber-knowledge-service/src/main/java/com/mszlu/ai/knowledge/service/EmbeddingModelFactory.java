package com.mszlu.ai.knowledge.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmbeddingModelFactory {

    public EmbeddingModel createEmbeddingModel(
            String provider,
            String modelName,
            String apiKey,
            String baseUrl,
            Integer embeddingDimension) {
        return switch (provider.toLowerCase()) {
            case "openai":
                yield createOpenAIModel(modelName, apiKey, baseUrl, embeddingDimension);
            case "dashscope", "qwen":
                yield createDashscopeModel(modelName, apiKey, baseUrl, embeddingDimension);
            default:
                yield createOpenAIModel(modelName, apiKey, baseUrl, embeddingDimension);
        };
    }

    private EmbeddingModel createDashscopeModel(String modelName,
                                                String apiKey,
                                                String baseUrl,
                                                Integer embeddingDimension) {
        DashScopeApi api = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .model(modelName)
                .dimensions(embeddingDimension)
                .build();
        return new DashScopeEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    private EmbeddingModel createOpenAIModel(String modelName,
                                             String apiKey,
                                             String baseUrl,
                                             Integer embeddingDimension) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelName)
                .dimensions(embeddingDimension)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}

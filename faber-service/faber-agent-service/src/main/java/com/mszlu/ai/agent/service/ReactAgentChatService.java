package com.mszlu.ai.agent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.agent.config.ChatModelConfig;
import com.mszlu.ai.agent.dto.AgentMessage;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.agent.feign.LlmServiceClient;
import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactAgentChatService {
    private final CommonService commonService;
    private final LlmServiceClient llmServiceClient;
    private final ObjectMapper objectMapper;
    private final ChatModelConfig chatModelConfig;
    public Flux<String> agentMessage(UUID agentId, String message, UUID sessionId) {
        UUID userId = UserContext.getUserId();
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        //异步处理消息
        processAgentMessageAsync(userId, agentId, message, sessionId, sink);
        return sink.asFlux();
    }

    private void processAgentMessageAsync(UUID userId,
                                          UUID agentId,
                                          String message,
                                          UUID sessionId,
                                          Sinks.Many<String> sink) {
        try {

        Agent agent = commonService.getAgentById(userId, agentId);
        if (agent == null) {
            sink.tryEmitNext(AgentMessage.buildErrMessage("system","Agent not found"));
            sink.tryEmitComplete();
            return;
        }
        //构建ChatModel
        ChatModel chatModel = buildChatModel(agent);
        //构建reactAgent并执行
        executeReactAgent(chatModel, agent.getSystemPrompt(), message, agent.getName(), sink);

        }catch (Exception e){
            sink.tryEmitNext(AgentMessage.buildErrMessage("system","Agent error: " + e.getMessage()));
            sink.tryEmitComplete();
        }
    }

    private void executeReactAgent(ChatModel chatModel,
                                   String systemPrompt,
                                   String userMessage,
                                   String agentName,
                                   Sinks.Many<String> sink) {
        //构建reactAgent
        ReactAgent reactAgent = ReactAgent.builder()
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .name(agentName)
                .returnReasoningContents(true)
                .build();
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .build();
        //前端根据id和steps标识消息块
        var ref = new Object(){
            int steps = 0;
        };
        try {
            reactAgent.stream(userMessage, runnableConfig)
                    .doOnNext(output -> {
                        try {
                            //处理大模型输出的内容
                            if (output != null) {
                                String content = null;
                                if (output instanceof StreamingOutput<?> streamingOutput) {
                                    OutputType outputType = streamingOutput.getOutputType();
                                    if (outputType == OutputType.AGENT_MODEL_STREAMING) {
                                        content = streamingOutput.message().getText();
                                        if (streamingOutput.message() instanceof AssistantMessage message) {
                                            Object reasoningContent = message.getMetadata().get("reasoningContent");
                                            String id = message.getMetadata().get("id").toString();
                                            String messageJson = null;
                                            if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                                                //思考内容
                                                messageJson = AgentMessage.buildReasoningMessage(id,
                                                        ref.steps,
                                                        agentName,
                                                        "",
                                                        reasoningContent.toString());
                                            } else {
                                                if (content != null && !content.isEmpty()) {
                                                    messageJson = AgentMessage.buildMessage(id,
                                                            ref.steps,
                                                            agentName,
                                                            "",
                                                            content);
                                                }
                                            }
                                            if (messageJson != null) {
                                                sink.tryEmitNext(messageJson);
                                            }
                                        }
                                    }
                                }
                            }
                        }catch (Exception e){
                            sink.tryEmitNext(AgentMessage.buildErrMessage("system","Agent error: " + e.getMessage()));
                            sink.tryEmitComplete();
                        }
                    })
                    .doOnError(e -> {
                        sink.tryEmitNext(AgentMessage.buildErrMessage("system","Agent error: " + e.getMessage()));
                        sink.tryEmitComplete();
                    }).doOnComplete(sink::tryEmitComplete).subscribe();
        } catch (Exception e) {
            sink.tryEmitNext(AgentMessage.buildErrMessage("system","Agent error: " + e.getMessage()));
            sink.tryEmitComplete();
        }
    }

    private ChatModel buildChatModel(Agent agent) {
        //需要通过feign远程调用llm服务 获取到使用的模型信息
        Result<LlmServiceClient.ProviderConfigResponse> providerConfig =
                llmServiceClient.getProviderConfig(
                        agent.getModelProvider(),
                        agent.getModelName());
        if (providerConfig == null || providerConfig.getData() == null){
            throw new RuntimeException("Provider config not found");
        }
        LlmServiceClient.ProviderConfigResponse providerConfigData = providerConfig.getData();
        //解析其中的配置
        double temperature = 0.7;
        int maxTokens = 1024;
        if (providerConfigData.getParameters() != null){
            try {
                Map<String,Object> parameters = objectMapper.readValue(agent.getModelParameters(), Map.class);
                if (parameters.containsKey("temperature")){
                    temperature = ((Number) parameters.get("temperature")).doubleValue();
                }
                if (parameters.containsKey("maxTokens")){
                    maxTokens = ((Number) parameters.get("maxTokens")).intValue();
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return chatModelConfig.createModel(
                providerConfigData.getProvider(),
                agent.getModelName(),
                providerConfigData.getApiBase(),
                providerConfigData.getApiKey(),
                temperature,
                maxTokens
        );
    }
}

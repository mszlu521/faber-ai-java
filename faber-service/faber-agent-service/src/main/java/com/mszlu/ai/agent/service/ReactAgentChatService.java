package com.mszlu.ai.agent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.agent.config.ChatModelConfig;
import com.mszlu.ai.agent.dto.AgentMessage;
import com.mszlu.ai.agent.dto.ToolDTO;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.agent.feign.LlmServiceClient;
import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.core.tools.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ReAct智能体聊天服务
 * <p>
 * 基于Spring AI Alibaba的Graph框架实现ReAct(Reasoning + Acting)智能体对话。
 * ReAct模式让大模型能够：
 * <ul>
 *   <li><b>Reason(推理)</b>：分析问题、拆解任务、思考解决步骤</li>
 *   <li><b>Act(行动)</b>：调用工具、查询知识库、执行外部操作</li>
 * </ul>
 * 该类负责构建ChatModel、配置ReAct Agent、处理流式输出，并将结果通过Sink推送给前端。
 * </p>
 *
 * @author mszlu
 * @since 1.0.0
 */
@Service                        // Spring注解：标识为服务层组件，由Spring容器管理生命周期
@Slf4j                          // Lombok注解：自动生成日志对象log
@RequiredArgsConstructor        // Lombok注解：为所有final字段生成构造方法，实现构造器注入
public class ReactAgentChatService {

    /**
     * 通用业务服务
     * <p>用于查询Agent配置信息、验证用户权限等</p>
     */
    private final CommonService commonService;

    /**
     * LLM服务Feign客户端
     * <p>通过OpenFeign远程调用faber-llm-service服务，获取模型提供商配置信息</p>
     */
    private final LlmServiceClient llmServiceClient;

    /**
     * Jackson对象映射器
     * <p>用于JSON字符串与Java对象之间的互相转换，解析模型参数配置</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 聊天模型配置
     * <p>根据提供商类型(OpenAI、阿里云、智谱等)创建对应的ChatModel实例</p>
     */
    private final ChatModelConfig chatModelConfig;
    private final ToolRegistry toolRegistry;

    /**
     * 智能体消息处理入口
     * <p>
     * 采用<b>响应式编程</b>模式，使用Reactor的Sink机制实现异步流式输出：
     * <ol>
     *   <li>创建多播Sink，支持背压缓冲</li>
     *   <li>启动异步线程处理实际对话逻辑（避免阻塞主线程）</li>
     *   <li>立即返回Flux流，前端可实时接收数据</li>
     * </ol>
     * </p>
     *
     * @param agentId   智能体唯一标识
     * @param message   用户发送的消息内容
     * @param sessionId 会话唯一标识，用于维护多轮对话上下文
     * @return Flux<String> 流式响应，每个元素为JSON格式的消息片段
     */
    public Flux<String> agentMessage(UUID agentId, String message, UUID sessionId) {
        // 获取当前登录用户ID，用于后续的权限校验
        UUID userId = UserContext.getUserId();

        // 创建多播Sink，支持多个订阅者，带背压缓冲防止消费者处理不过来导致数据丢失
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        // 异步处理消息：将耗时的大模型调用放到独立线程，不阻塞当前方法返回Flux
        processAgentMessageAsync(userId, agentId, message, sessionId, sink);

        // 立即返回Flux流，调用方可以开始订阅接收数据
        return sink.asFlux();
    }

    /**
     * 异步处理智能体消息
     * <p>
     * 该方法在后台线程执行完整的对话流程：
     * <ol>
     *   <li>查询并校验Agent信息</li>
     *   <li>构建ChatModel（远程获取模型配置）</li>
     *   <li>执行ReAct Agent流式对话</li>
     * </ol>
     * 所有异常都会被捕获并通过Sink推送错误信息，确保前端能收到反馈。
     * </p>
     *
     * @param userId    当前用户ID
     * @param agentId   智能体ID
     * @param message   用户消息
     * @param sessionId 会话ID
     * @param sink      响应式Sink，用于向Flux流推送数据
     */
    private void processAgentMessageAsync(UUID userId,
                                          UUID agentId,
                                          String message,
                                          UUID sessionId,
                                          Sinks.Many<String> sink) {
        try {
            // 1. 查询Agent信息，同时校验该Agent是否属于当前用户
            Agent agent = commonService.getAgentById(userId, agentId);
            if (agent == null) {
                // Agent不存在或无权限：推送错误信息并结束流
                sink.tryEmitNext(AgentMessage.buildErrMessage("system", "Agent not found"));
                sink.tryEmitComplete();
                return;
            }

            // 2. 构建ChatModel：通过Feign远程获取模型配置，创建对应提供商的ChatModel实例
            ChatModel chatModel = buildChatModel(agent);

            // 3. 构建ReAct Agent并执行流式对话
            //    传入：模型实例、系统提示词、用户消息、Agent名称、Sink
            executeReactAgent(chatModel, agent, agent.getSystemPrompt(), message, agent.getName(), sink);

        } catch (Exception e) {
            // 捕获所有异常，确保Sink正确关闭，避免前端一直等待
            log.error("Agent message processing error", e);
            sink.tryEmitNext(AgentMessage.buildErrMessage("system", "Agent error: " + e.getMessage()));
            sink.tryEmitComplete();
        }
    }

    /**
     * 执行ReAct智能体流式对话
     * <p>
     * 核心逻辑：
     * <ol>
     *   <li>使用Builder模式构建ReactAgent实例</li>
     *   <li>配置RunnableConfig（可扩展线程ID、回调函数等）</li>
     *   <li>订阅流式输出，区分"思考内容"和"正式回复"</li>
     *   <li>通过Sink实时推送给前端</li>
     * </ol>
     * </p>
     *  @param chatModel     Spring AI的ChatModel接口实例，封装了具体LLM的调用能力
     *
     * @param agent
     * @param systemPrompt 系统提示词，定义Agent的角色、能力边界和行为规范
     * @param userMessage  用户的输入消息
     * @param agentName    Agent的显示名称，用于构建消息体返回给前端展示
     * @param sink         响应式Sink，实时推送流式输出
     */
    private void executeReactAgent(ChatModel chatModel,
                                   Agent agent,
                                   String systemPrompt,
                                   String userMessage,
                                   String agentName,
                                   Sinks.Many<String> sink) {
        // 使用Builder模式构建ReAct Agent
        // returnReasoningContents=true 表示需要返回推理/思考过程（如DeepSeek的reasoning_content）
        List<ToolCallback> toolCallbacks = buildToolCallbacks(agent);
        ReactAgent reactAgent = ReactAgent.builder()
                .model(chatModel)                       // 设置底层大模型
                .systemPrompt(systemPrompt)             // 设置系统提示词
                .name(agentName)                        // 设置Agent名称
                .returnReasoningContents(true)          // 开启推理内容返回（思考过程可见）
                .tools(toolCallbacks)
                .build();

        // 构建运行配置，可在此设置threadId（用于持久化对话历史）、回调函数等
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .build();

        // 使用匿名对象包装steps计数器，使其在Lambda表达式中可修改
        // 前端通过steps字段区分同一轮对话中的不同消息块（思考块、回复块、工具调用块等）
        var ref = new Object() {
            int steps = 0;
        };

        try {
            // 启动流式对话：发送用户消息，订阅返回的流式输出
            reactAgent.stream(userMessage, runnableConfig)
                    .doOnNext(output -> {
                        // 处理每一个流式输出事件
                        try {
                            if (output != null) {
                                String content = null;
                                // 判断输出类型是否为流式输出
                                if (output instanceof StreamingOutput<?> streamingOutput) {
                                    OutputType outputType = streamingOutput.getOutputType();
                                    // 只处理Agent模型流式输出类型（过滤掉工具调用等其他类型）
                                    if (outputType == OutputType.AGENT_MODEL_STREAMING) {
                                        // 获取消息文本内容
                                        content = streamingOutput.message().getText();

                                        // 判断消息是否为AssistantMessage类型
                                        if (streamingOutput.message() instanceof AssistantMessage message) {
                                            // 从元数据中提取推理内容（如DeepSeek的reasoning_content）
                                            Object reasoningContent = message.getMetadata().get("reasoningContent");
                                            // 提取消息唯一ID，前端用于标识和去重
                                            String id = message.getMetadata().get("id").toString();
                                            String messageJson = null;

                                            // 场景1：存在推理内容（思考过程）
                                            if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                                                // 构建推理消息：展示模型的思考过程
                                                messageJson = AgentMessage.buildReasoningMessage(
                                                        id,
                                                        ref.steps,              // 当前步骤序号
                                                        agentName,              // Agent名称
                                                        "",                     // 工具名称（推理阶段为空）
                                                        reasoningContent.toString()  // 推理内容
                                                );
                                            } else {
                                                // 场景2：存在正式回复内容
                                                if (content != null && !content.isEmpty()) {
                                                    // 构建正式回复消息
                                                    messageJson = AgentMessage.buildMessage(
                                                            id,
                                                            ref.steps,
                                                            agentName,
                                                            "",                     // 工具名称
                                                            content                 // 回复内容
                                                    );
                                                }
                                            }

                                            // 如果构建了有效消息，通过Sink推送给订阅者
                                            if (messageJson != null) {
                                                sink.tryEmitNext(messageJson);
                                            }
                                            //处理工具调用
                                            if (message.hasToolCalls()){
                                                for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                                                    ref.steps++;
                                                    String toolId = toolCall.id();
                                                    String toolName = toolCall.name();
                                                    String toolJson = AgentMessage.buildToolCallMessage(
                                                            id,
                                                            ref.steps,
                                                            agentName,
                                                            toolId,
                                                            toolName,
                                                            toolCall.arguments()
                                                    );
                                                    sink.tryEmitNext(toolJson);
                                                }
                                            }
                                        }
                                    }
                                    //工具调用完成
                                    if (outputType == OutputType.AGENT_TOOL_FINISHED) {
                                        if (streamingOutput.message() instanceof ToolResponseMessage message){
                                            for (ToolResponseMessage.ToolResponse response : message.getResponses()){
                                                ref.steps++;
                                                String toolResponseJson = AgentMessage.buildToolResponseMessage(
                                                        UUID.randomUUID().toString(),
                                                        ref.steps,
                                                        agentName,
                                                        response.id(),
                                                        response.name(),
                                                        response.responseData()
                                                );
                                                sink.tryEmitNext(toolResponseJson);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 处理单条消息异常，不影响整个流
                            log.error("Stream output processing error", e);
                            sink.tryEmitNext(AgentMessage.buildErrMessage("system", "Agent error: " + e.getMessage()));
                            sink.tryEmitComplete();
                        }
                    })
                    // 处理流级别错误
                    .doOnError(e -> {
                        log.error("Agent stream error", e);
                        sink.tryEmitNext(AgentMessage.buildErrMessage("system", "Agent error: " + e.getMessage()));
                        sink.tryEmitComplete();
                    })
                    // 流正常完成时关闭Sink
                    .doOnComplete(sink::tryEmitComplete)
                    .subscribe();  // 订阅启动流式处理
        } catch (Exception e) {
            // 捕获stream()方法本身的异常
            log.error("Agent execution error", e);
            sink.tryEmitNext(AgentMessage.buildErrMessage("system", "Agent error: " + e.getMessage()));
            sink.tryEmitComplete();
        }
    }

    private List<ToolCallback> buildToolCallbacks(Agent agent) {
        //先查出关联的tool
        List<ToolDTO> agentTools = commonService.getAgentTools(agent.getId());
        List<String> toolNames = agentTools.stream().map(ToolDTO::getName).toList();
        List<Object> instances = toolRegistry.getBeanInstances(toolNames);
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(instances.toArray())
                .build();
        return Arrays.asList( provider.getToolCallbacks());
    }

    /**
     * 构建ChatModel实例
     * <p>
     * 通过Feign远程调用LLM服务获取模型提供商配置，然后解析模型参数，
     * 最终调用ChatModelConfig创建对应提供商的ChatModel。
     * </p>
     *
     * @param agent 智能体实体，包含modelProvider、modelName、modelParameters等配置
     * @return ChatModel Spring AI标准的聊天模型接口实例
     * @throws RuntimeException 当提供商配置不存在或参数解析失败时抛出
     */
    private ChatModel buildChatModel(Agent agent) {
        // 通过Feign远程调用faber-llm-service，获取模型提供商的详细配置
        Result<LlmServiceClient.ProviderConfigResponse> providerConfig =
                llmServiceClient.getProviderConfig(
                        agent.getModelProvider(),   // 提供商代码，如"openai"、"aliyun"、"zhipu"
                        agent.getModelName()        // 模型名称，如"gpt-4"、"qwen-turbo"
                );

        // 校验远程调用结果
        if (providerConfig == null || providerConfig.getData() == null) {
            throw new RuntimeException("Provider config not found");
        }
        LlmServiceClient.ProviderConfigResponse providerConfigData = providerConfig.getData();

        // 设置默认参数值
        double temperature = 0.7;   // 温度参数：控制输出随机性，0-1之间，越大越随机
        int maxTokens = 1024;       // 最大生成token数，限制回复长度

        // 解析Agent中配置的模型参数（JSON格式字符串）
        if (providerConfigData.getParameters() != null) {
            try {
                // 将JSON参数字符串反序列化为Map
                Map<String, Object> parameters = objectMapper.readValue(agent.getModelParameters(), Map.class);

                // 读取temperature参数，控制模型输出的创造性和确定性
                if (parameters.containsKey("temperature")) {
                    temperature = ((Number) parameters.get("temperature")).doubleValue();
                }
                // 读取maxTokens参数，限制单次回复的最大长度
                if (parameters.containsKey("maxTokens")) {
                    maxTokens = ((Number) parameters.get("maxTokens")).intValue();
                }
            } catch (JsonProcessingException e) {
                // JSON解析失败，抛出运行时异常
                throw new RuntimeException("Failed to parse model parameters", e);
            }
        }

        // 调用配置工厂创建ChatModel实例
        return chatModelConfig.createModel(
                providerConfigData.getProvider(),   // 提供商类型
                agent.getModelName(),               // 模型名称
                providerConfigData.getApiBase(),    // API基础地址
                providerConfigData.getApiKey(),     // API密钥
                temperature,                        // 温度参数
                maxTokens                           // 最大token数
        );
    }
}

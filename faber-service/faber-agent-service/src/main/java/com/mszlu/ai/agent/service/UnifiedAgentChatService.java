package com.mszlu.ai.agent.service;

import com.mszlu.ai.agent.dto.AgentMessage;
import com.mszlu.ai.agent.dto.AgentMessageRequest;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 统一智能体聊天服务
 * <p>
 * 该类作为智能体聊天的统一入口，负责根据智能体的不同模式(mode)路由到对应的处理服务。
 * 目前主要支持"普通模式"(general)，后续可扩展支持其他模式如"流式模式"、"多轮对话模式"等。
 * </p>
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>用户身份校验与Agent权限验证</li>
 *   <li>根据Agent的mode字段进行服务路由</li>
 *   <li>统一封装响应格式</li>
 * </ul>
 *
 * @author mszlu
 * @since 1.0.0
 */
@Slf4j                          // Lombok注解：自动生成日志对象log，用于记录日志信息
@Service                        // Spring注解：标识该类为服务层组件，由Spring容器管理
@RequiredArgsConstructor        // Lombok注解：为所有final字段生成构造方法，实现依赖注入
public class UnifiedAgentChatService {

    /**
     * 通用业务服务
     * <p>用于查询Agent信息、验证用户权限等通用操作</p>
     */
    private final CommonService commonService;

    /**
     * ReAct智能体聊天服务
     * <p>处理ReAct(Reasoning + Acting)模式的智能体对话，支持流式响应</p>
     */
    private final ReactAgentChatService reactAgentChatService;

    /**
     * 统一聊天入口方法
     * <p>
     * 处理流程：
     * <ol>
     *   <li>从用户上下文中获取当前登录用户ID</li>
     *   <li>根据用户ID和AgentID查询Agent信息，验证用户是否有权访问该Agent</li>
     *   <li>获取Agent的模式(mode)，如果未设置则默认为"general"</li>
     *   <li>根据mode路由到对应的处理逻辑</li>
     * </ol>
     * </p>
     *
     * @param request 智能体消息请求对象，包含agentId、message、sessionId等信息
     * @return Flux<String> 流式响应，每个元素为一段JSON格式的消息字符串
     *         如果Agent不存在，返回包含错误信息的Flux流
     */
    public Flux<String> chat(AgentMessageRequest request) {
        // 1. 获取当前登录用户的唯一标识(UUID)
        UUID userId = UserContext.getUserId();

        // 2. 从请求中获取目标Agent的ID
        UUID agentId = request.getAgentId();

        // 3. 查询Agent信息，同时验证该Agent是否属于当前用户（权限校验）
        Agent agent = commonService.getAgentById(userId, agentId);

        // 4. 如果Agent不存在或无权限访问，返回错误信息
        if (agent == null) {
            // 构建标准错误消息，"system"表示系统级别的错误
            return Flux.just(AgentMessage.buildErrMessage("system", "Agent not found"));
        }

        // 5. 获取Agent的运行模式，如果数据库中未配置则默认使用"general"模式
        String mode = agent.getAgentMode() == null ? "general" : agent.getAgentMode();

        // 6. 根据mode选择对应的服务进行处理
        // 当前仅实现了普通模式(general)，后续可扩展其他模式：
        // - "stream": 纯流式输出模式
        // - "multi-turn": 多轮对话记忆模式
        // - "function-call": 函数调用模式
        return handleGeneralMode(agent, request);
    }

    /**
     * 处理普通模式(general)的智能体对话
     * <p>
     * 普通模式采用ReAct(Reasoning + Acting)架构：
     * <ul>
     *   <li>Reasoning：智能体进行推理思考</li>
     *   <li>Acting：根据推理结果执行相应动作（如调用工具、查询知识库等）</li>
     * </ul>
     * 响应以流式(Flux)方式返回，实现打字机效果，提升用户体验。
     * </p>
     *
     * @param agent   查询到的Agent实体对象，包含模型配置、提示词模板等信息
     * @param request 用户的消息请求
     * @return Flux<String> 流式返回智能体的回复内容
     */
    private Flux<String> handleGeneralMode(Agent agent, AgentMessageRequest request) {
        // 调用ReAct智能体服务处理消息，传入：
        // - agentId: 智能体唯一标识
        // - message: 用户输入的消息内容
        // - sessionId: 会话ID，用于维护多轮对话的上下文
        return reactAgentChatService.agentMessage(
                request.getAgentId(),
                request.getMessage(),
                request.getSessionId()
        );
    }
}

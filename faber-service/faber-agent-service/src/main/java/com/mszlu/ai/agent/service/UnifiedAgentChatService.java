package com.mszlu.ai.agent.service;

import com.mszlu.ai.agent.dto.AgentMessage;
import com.mszlu.ai.agent.dto.AgentMessageRequest;
import com.mszlu.ai.agent.entity.Agent;
import com.mszlu.ai.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedAgentChatService {
    private final CommonService commonService;
    private final ReactAgentChatService reactAgentChatService;
    public Flux<String> chat(AgentMessageRequest request) {
        UUID userId = UserContext.getUserId();
        UUID agentId = request.getAgentId();
        Agent agent = commonService.getAgentById(userId, agentId);
        if (agent == null) {
            return Flux.just(AgentMessage.buildErrMessage("system","Agent not found"));
        }
        String mode = agent.getAgentMode() == null ? "general" : agent.getAgentMode();
        //根据mode 选择对应的服务 我们这里先实现普通的形式
        return handleGeneralMode(agent, request);
    }

    private Flux<String> handleGeneralMode(Agent agent, AgentMessageRequest request) {
        return reactAgentChatService.agentMessage(
                request.getAgentId(),
                request.getMessage(),
                request.getSessionId()
        );
    }
}

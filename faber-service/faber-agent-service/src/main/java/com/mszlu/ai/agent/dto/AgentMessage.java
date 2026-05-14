package com.mszlu.ai.agent.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Data
@Builder
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    private String id;

    private int steps;
    //动作类型 agent_answer session_created error等
    private String action;
    private String agentName;
    private String toolName;
    private String toolId;
    //工具动作 agent_answer返回，call代表准备调用 response代表工具返回结果
    private String toolAction;
    private Boolean isErr;
    private String content;
    private String reasoningContent;
    private String sessionId;
    private String sessionTitle;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    //构建各种类型的消息

    public static String buildErrMessage(String agentName, String errMsg){
        AgentMessage agentMessage = AgentMessage.builder()
                .id(UUID.randomUUID().toString())
                .action("agent_answer") //这种类型在前端会处理成对话，错误我们以对话形式展示
                .agentName(agentName)
                .content(errMsg)
                .isErr(true)
                .build();
        return toJson(agentMessage);
    }
    //我们在这里构建各种形式的消息，比如思考消息，工具调用消息，工具返回消息，会话创建消息，会话结束消息，错误消息等等
    public static String buildErrMessage(String id, String agentName, String errMsg) {
        AgentMessage msg = AgentMessage.builder()
                .id(id)
                .action("agent_answer")
                .agentName(agentName)
                .isErr(true)
                .content(errMsg)
                .build();
        return toJson(msg);
    }

    /**
     * 构建推理/思考消息
     */
    public static String buildReasoningMessage(String id,
                                               int steps,
                                               String agentName,
                                               String toolName,
                                               String reasoningContent) {
        AgentMessage msg = AgentMessage.builder()
                .id(id)
                .action("agent_answer")
                .steps(steps)
                .agentName(agentName)
                .toolName(toolName)
                .reasoningContent(reasoningContent)
                .build();
        return toJson(msg);
    }

    /**
     * 构建普通消息
     */
    public static String buildMessage(String id, int steps,String agentName, String toolName, String content) {
        AgentMessage msg = AgentMessage.builder()
                .id(id)
                .steps(steps)
                .action("agent_answer")
                .agentName(agentName)
                .toolName(toolName)
                .content(content)
                .build();
        return toJson(msg);
    }

    public static String buildToolCallMessage(String id,
                                              int steps,
                                              String agentName,
                                              String toolId ,
                                              String toolName,
                                              String content) {
        AgentMessage msg = AgentMessage.builder()
                .id(id)
                .steps(steps)
                .action("agent_answer")
                .toolAction("call")
                .toolId(toolId)
                .agentName(agentName)
                .toolName(toolName)
                .content(content)
                .build();
        return toJson(msg);
    }
    public static String buildToolResponseMessage(String id,
                                                  int steps,
                                                  String agentName,
                                                  String toolId ,
                                                  String toolName,
                                                  String content) {
        AgentMessage msg = AgentMessage.builder()
                .id(id)
                .steps(steps)
                .action("agent_answer")
                .toolAction("response")
                .toolId(toolId)
                .agentName(agentName)
                .toolName(toolName)
                .content(content)
                .build();
        return toJson(msg);
    }

    /**
     * 构建会话创建消息
     */
    public static String buildSessionCreatedMessage(String sessionId, String title) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "action", "session_created",
                    "sessionId", sessionId,
                    "title", title
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to build session created message", e);
            return "{}";
        }
    }
    private static String toJson(AgentMessage agentMessage) {
        try {
            return objectMapper.writeValueAsString(agentMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.mszlu.ai.agent.controller;

import com.mszlu.ai.agent.dto.*;
import com.mszlu.ai.agent.service.AgentService;
import com.mszlu.ai.agent.service.UnifiedAgentChatService;
import com.mszlu.ai.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final UnifiedAgentChatService unifiedAgentChatService;

    @PostMapping("/list")
    public Result<AgentListResponse> listAgents(@RequestBody AgentQueryListRequest request) {
        return Result.success(agentService.listAgents(request.getParams()));
    }
    @PostMapping("/create")
    public Result<AgentResponse> createAgent(@RequestBody AgentCreateRequest request) {
        return Result.success(agentService.createAgent(request));
    }
    @GetMapping("/{id}")
    public Result<AgentResponse> getAgent(@PathVariable UUID id) {
        return Result.success(agentService.getAgent(id));
    }
    @PutMapping("/update")
    public Result<AgentResponse> updateAgent(@RequestBody AgentUpdateRequest request) {
        return Result.success(agentService.updateAgent(request));
    }
    @DeleteMapping("/{id}")
    public Result<Void> deleteAgent(@PathVariable UUID id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AgentMessageRequest request) {
        return unifiedAgentChatService.chat(request)
                .map(message -> ServerSentEvent.<String>builder()
                        .data(message)
                        .build());
    }
}

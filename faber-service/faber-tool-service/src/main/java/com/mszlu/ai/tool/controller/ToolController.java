package com.mszlu.ai.tool.controller;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.tool.dto.*;
import com.mszlu.ai.tool.entity.Tool;
import com.mszlu.ai.tool.service.ToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolService toolService;

    /**
     * 创建tool
     */
    @PostMapping
    public Result<ToolResponse> createTool(@Valid @RequestBody ToolCreateRequest request) {
        ToolResponse toolResponse = toolService.createTool(request);
        return Result.success(toolResponse);
    }
    @GetMapping
    public Result<ToolListResponse> listTools(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type
    ) {
        ToolListRequest request = new ToolListRequest();
        request.setName(name);
        request.setType(type);
        request.setPage(page);
        request.setPageSize(pageSize);
        return Result.success(toolService.listTools(request));
    }
    @DeleteMapping("/{id}")
    public Result<Void> deleteTool(@PathVariable UUID id) {
        toolService.deleteTool(id);
        return Result.success();
    }
    @PostMapping("/{id}/test")
    public Result<ToolTestResponse> testTool(@PathVariable UUID id, @Valid @RequestBody ToolTestRequest request) {
        return Result.success(toolService.testTool(id,request));
    }
    @GetMapping("/{id}")
    public Result<Tool> getTool(@PathVariable UUID id) {
        return Result.success(toolService.getTool(id));
    }
}

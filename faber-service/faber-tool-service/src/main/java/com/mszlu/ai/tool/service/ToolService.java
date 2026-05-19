package com.mszlu.ai.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.result.ResultCode;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.core.tools.metadata.ToolMetadata;
import com.mszlu.ai.core.tools.registry.ToolRegistry;
import com.mszlu.ai.tool.dto.*;
import com.mszlu.ai.tool.entity.AgentTool;
import com.mszlu.ai.tool.entity.Tool;
import com.mszlu.ai.tool.mapper.AgentToolMapper;
import com.mszlu.ai.tool.mapper.ToolMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolService {
    private final ToolMapper toolMapper;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final AgentToolMapper agentToolMapper;
    public ToolResponse createTool(@Valid ToolCreateRequest request) {
        UUID userId = UserContext.getUserId();
        String toolName = request.getName();
        //检查tool是否在系统重注册
        if (!toolRegistry.contains(toolName)){
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        //检查同一个用户是否已经存在同名的tool
        LambdaQueryWrapper<Tool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tool::getCreatorId, userId);
        queryWrapper.eq(Tool::getName, toolName);
        if (toolMapper.selectCount(queryWrapper) > 0){
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS);
        }
        ToolMetadata metadata = toolRegistry.getMetadata(toolName);
        Tool tool = new Tool();
        tool.setCreatorId(userId);
        tool.setName(toolName);
        tool.setDescription(metadata.getDescription());
        tool.setIsEnable(request.isEnabled());
        tool.setParametersSchema(metadata.getParametersSchema());
        tool.setMcpConfig(null);
        //后续我们添加mcp工具
        tool.setToolType("system");
        toolMapper.insert(tool);
        return toResponse(tool);
    }

    private ToolResponse toResponse(Tool tool) {
        Map<String,Object> parametersSchema = null;
        if (tool.getParametersSchema() != null) {
            try {
                parametersSchema = objectMapper.readValue(tool.getParametersSchema(), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse parameters schema for tool: {}", tool.getId());
            }
        }
        McpConfigResponse mcpConfig = null;
        if (tool.getMcpConfig() != null){
            try {
                mcpConfig = objectMapper.readValue(tool.getMcpConfig(), McpConfigResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse mcp config for tool: {}", tool.getId());
            }
        }
        return ToolResponse.builder()
                .id(tool.getId())
                .creatorId(tool.getCreatorId())
                .name(tool.getName())
                .description(tool.getDescription())
                .toolType(tool.getToolType())
                .parametersSchema(parametersSchema)
                .mcpConfig(mcpConfig)
                .isEnabled(tool.getIsEnabled())
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .build();
    }

    public ToolListResponse listTools(ToolListRequest request) {
        int page = request.getPage();
        int pageSize = request.getPageSize();
        Page<Tool> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Tool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tool::getCreatorId, UserContext.getUserId());
        queryWrapper.like(StringUtils.isNotBlank(request.getName()), Tool::getName, request.getName());
        queryWrapper.eq(StringUtils.isNotBlank(request.getType()), Tool::getToolType, request.getType());
        Page<Tool> toolPage = toolMapper.selectPage(pageParam, queryWrapper);
        ToolListResponse response = new ToolListResponse();
        response.setList(toolPage.getRecords().stream().map(this::toToolItem).toList());
        response.setTotal(toolPage.getTotal());
        response.setCurrentPage(request.getPage());
        response.setPageSize(request.getPageSize());
        return response;
    }

    private ToolListResponse.ToolItem toToolItem(Tool tool) {
        ToolListResponse.ToolItem item = new ToolListResponse.ToolItem();
        if (tool.getParametersSchema() != null){
            try {
                Map<String,ToolParameterSchema> schemaMap = objectMapper.readValue(tool.getParametersSchema(), Map.class);
                item.setParametersSchema(schemaMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (tool.getMcpConfig() != null){
            try {
                item.setMcpConfig(objectMapper.readValue(tool.getMcpConfig(), McpConfigResponse.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        item.setId(tool.getId());
        item.setName(tool.getName());
        item.setDescription(tool.getDescription());
        item.setToolType(tool.getToolType());
        item.setIsEnable(tool.getIsEnabled());
        item.setCreatedAt(tool.getCreatedAt());
        item.setUpdatedAt(tool.getUpdatedAt());
        item.setDeletedAt(tool.getDeletedAt());
        item.setCreatorId(tool.getCreatorId());
        return item;
    }

    @Transactional
    public void deleteTool(UUID id) {
        toolMapper.deleteById(id);
        agentToolMapper.delete(new LambdaQueryWrapper<AgentTool>().eq(AgentTool::getToolId, id));
    }

    public ToolTestResponse testTool(UUID id, @Valid ToolTestRequest request) {
        Tool tool = toolMapper.selectById(id);
        if (tool == null){
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        String toolName = tool.getName();
        Object bean = toolRegistry.getBeanInstance(toolName);
        Method method = toolRegistry.getMethod(toolName);
        if (bean == null || method == null){
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Map<String, Object> params = request.getParams();
        Object[] args = buildMethodArgs(method, params);
        method.setAccessible(true);
        Object result;
        try {
            result = method.invoke(bean, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ToolTestResponse response = ToolTestResponse.builder()
                .success(true)
                .msg("success")
                .build();
        if (result instanceof Map<?,?>){
            response.setData((Map<String, Object>) result);
        }else if (result != null){
            try {
                response.setData(objectMapper.convertValue(result, Map.class));
            }catch (Exception e){
                //string 字符串处理
                response.setData(Map.of("result",result));
            }
        }else{
            response.setData(Map.of());
        }
        return response;
    }

    private Object[] buildMethodArgs(Method method, Map<String, Object> params) {
        Parameter[] parameters = method.getParameters();
        List<Object> args = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            Object value = params != null ? params.get(paramName) : null;
            args.add(convertValue(value, parameter.getType()));
        }
        return args.toArray();
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == double.class) return 0.0;
                if (targetType == float.class) return 0.0f;
                if (targetType == short.class) return (short) 0;
                if (targetType == byte.class) return (byte) 0;
            }
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return value.toString();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.toString());
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value.toString());
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value.toString());
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value.toString());
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value.toString());
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value.toString());
        }
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}", value, targetType.getName());
            return null;
        }
    }

    public Tool getTool(UUID id) {
        return toolMapper.selectById(id);
    }
}

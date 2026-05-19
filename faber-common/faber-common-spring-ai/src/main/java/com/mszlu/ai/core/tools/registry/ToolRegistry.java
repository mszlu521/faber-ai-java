package com.mszlu.ai.core.tools.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mszlu.ai.core.tools.metadata.ParameterInfo;
import com.mszlu.ai.core.tools.metadata.ToolMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
@Slf4j
public class ToolRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, ToolMetadata> metadataMap = new LinkedHashMap<>();
    private final Map<String, Object> beanInstanceMap = new LinkedHashMap<>();
    private final Map<String, Method> methodMap = new LinkedHashMap<>();
    @EventListener(ContextRefreshedEvent.class)
    public void conContextRefreshed(ContextRefreshedEvent  event){
        ApplicationContext applicationContext = event.getApplicationContext();
        if (applicationContext.getParent() != null){
            //不处理上下文
            return;
        }
        scanAndRegisterTools(applicationContext);
    }

    public synchronized void scanAndRegisterTools(ApplicationContext context) {
        metadataMap.clear();
        beanInstanceMap.clear();
        methodMap.clear();

        String[] beanDefinitionNames = context.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            Object bean = null;
            try {
                bean = context.getBean(beanName);
            }catch (Exception e){
                log.error("获取bean异常", e);
                continue;
            }
            Class<?> clazz = bean.getClass();
            for (Method method : getAllMethods(clazz)) {
                Tool tooAnnotation = method.getAnnotation(Tool.class);
                if (tooAnnotation == null) {
                    continue;
                }
                String toolName = tooAnnotation.name();
                if (toolName.isEmpty()){
                    toolName = method.getName();
                }
                if (metadataMap.containsKey(toolName)){
                    //重复的工具名
                    log.warn("重复的工具名: {}", toolName);
                    continue;
                }
                ToolMetadata metadata = buildMetadata(tooAnnotation,method, clazz.getName());
                metadataMap.put(toolName, metadata);
                beanInstanceMap.put(toolName, bean);
                methodMap.put(toolName, method);
            }
        }
        log.info("工具注册完成: {}", metadataMap.size());
    }

    private ToolMetadata buildMetadata(Tool tooAnnotation, Method method, String clazzName) {
        String toolName = tooAnnotation.name();
        if (toolName.isEmpty()){
            toolName = method.getName();
        }
        Map<String, ParameterInfo> parametersSchemaMap = new LinkedHashMap<>();
        List<ToolMetadata.ToolParameter> parameters = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            ToolParam toolParam = param.getAnnotation(ToolParam.class);
            String paramName = param.getName();
            String paramDesc = "";
            boolean paramRequired = true;
            if (toolParam != null) {
                paramDesc = toolParam.description();
                paramRequired = toolParam.required();
            }
            String paramType = mapJavaTypeToJsonSchemaType(param.getType());
            parameters.add(
                    ToolMetadata.ToolParameter.builder()
                            .name(paramName)
                            .description(paramDesc)
                            .type(paramType)
                            .required(paramRequired)
                            .build()
            );
            ParameterInfo parameterInfo = ParameterInfo.builder()
                    .type(paramType)
                    .desc(paramDesc)
                    .required(paramRequired)
                    .build();
            parametersSchemaMap.put(paramName, parameterInfo);
        }
        String schemaJson;
        try {
            schemaJson = objectMapper.writeValueAsString(parametersSchemaMap);
        }catch (Exception e){
            log.error("参数转换异常", e);
            schemaJson = "{}";
        }
        return ToolMetadata.builder()
                .name(toolName)
                .description(tooAnnotation.description())
                .sourceType("java")
                .beanClassName(clazzName)
                .methodName(method.getName())
                .parametersSchema(schemaJson)
                .parameters(parameters)
                .build();
    }

    private String mapJavaTypeToJsonSchemaType(Class<?> type) {
        if (type == String.class){
            return "string";
        }else if (type == Integer.class || type == int.class
                    || type == Long.class || type == long.class
                    || type == Short.class || type == short.class
                    || type == Byte.class || type == byte.class){
            return "integer";
        }else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class){
            return "number";
        }else if (type == Boolean.class || type == boolean.class){
            return "boolean";
        }else if (type.isArray() || Collections.class.isAssignableFrom(type)){
            return "array";
        }else if (Map.class.isAssignableFrom(type)){
            return "object";
        }else{
            return "object";
        }
    }

    private List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            methods.addAll(List.of(clazz.getDeclaredMethods()));
            clazz = clazz.getSuperclass();
        }
        return methods;
    }

    public ToolMetadata getMetadata(String toolName) {
        return metadataMap.get(toolName);
    }
    public Object getBeanInstance(String toolName) {
        return beanInstanceMap.get(toolName);
    }
    public Method getMethod(String toolName) {
        return methodMap.get(toolName);
    }
    public List<ToolMetadata> getAllMetadata() {
        return new ArrayList<>(metadataMap.values());
    }
    public boolean contains(String toolName) {
        if (metadataMap.containsKey(toolName)){
            return true;
        }
        return false;
    }

    public List<Object> getBeanInstances(List<String> toolNames) {
        List<Object> result = new ArrayList<>();
        Set<Object> uniqueBeanInstances = new HashSet<>();
        for (String toolName : toolNames) {
            Object beanInstance = beanInstanceMap.get(toolName);
            if (beanInstance != null && uniqueBeanInstances.add(beanInstance)){
                result.add(beanInstance);
            }
        }
        return result;
    }
}

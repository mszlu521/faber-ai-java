package com.mszlu.ai.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类 - 基于Jackson实现
 * 提供JSON序列化、反序列化工具方法
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 配置序列化特性
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 配置反序列化特性
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        
        // 注册Java时间模块，支持Java 8日期时间类型
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        
        // 设置日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        OBJECT_MAPPER.setDateFormat(dateFormat);
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转JSON字符串
     *
     * @param obj 待转换的对象
     * @return JSON字符串，如果对象为null则返回"null"
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败", e);
            throw new RuntimeException("对象转JSON字符串失败", e);
        }
    }

    /**
     * 对象转格式化的JSON字符串（便于阅读）
     *
     * @param obj 待转换的对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转格式化JSON字符串失败", e);
            throw new RuntimeException("对象转格式化JSON字符串失败", e);
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象，如果json为空则返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON字符串转对象失败, json: {}, class: {}", json, clazz.getName(), e);
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * JSON字符串转对象（支持复杂泛型）
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("JSON字符串转对象失败, json: {}", json, e);
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * JSON字符串转List
     *
     * @param json  JSON字符串
     * @param clazz 列表元素类型
     * @param <T>   泛型类型
     * @return List对象，如果json为空则返回null
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, 
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("JSON字符串转List失败, json: {}, class: {}", json, clazz.getName(), e);
            throw new RuntimeException("JSON字符串转List失败", e);
        }
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON字符串
     * @return Map对象，如果json为空则返回null
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("JSON字符串转Map失败, json: {}", json, e);
            throw new RuntimeException("JSON字符串转Map失败", e);
        }
    }

    /**
     * JSON字符串转Map（指定key和value类型）
     *
     * @param json      JSON字符串
     * @param keyClass  key类型
     * @param valueClass value类型
     * @param <K>       key泛型
     * @param <V>       value泛型
     * @return Map对象
     */
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, 
                    OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (IOException e) {
            log.error("JSON字符串转Map失败, json: {}", json, e);
            throw new RuntimeException("JSON字符串转Map失败", e);
        }
    }

    /**
     * 从输入流读取JSON并转换为对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 转换后的对象
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("从输入流读取JSON并转换对象失败, class: {}", clazz.getName(), e);
            throw new RuntimeException("从输入流读取JSON并转换对象失败", e);
        }
    }

    /**
     * 对象转字节数组
     *
     * @param obj 待转换的对象
     * @return 字节数组
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转字节数组失败", e);
            throw new RuntimeException("对象转字节数组失败", e);
        }
    }

    /**
     * 字节数组转对象
     *
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象
     */
    public static <T> T fromJsonBytes(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("字节数组转对象失败, class: {}", clazz.getName(), e);
            throw new RuntimeException("字节数组转对象失败", e);
        }
    }

    /**
     * 判断字符串是否是合法的JSON
     *
     * @param json 待判断的字符串
     * @return true-是JSON，false-不是JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 深拷贝对象（通过JSON序列化/反序列化实现）
     *
     * @param obj   源对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 深拷贝后的对象
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        String json = toJson(obj);
        return fromJson(json, clazz);
    }

    /**
     * 更新对象的属性（将source的非空属性复制到target）
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    泛型类型
     * @return 更新后的目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T updateProperties(Object source, T target) {
        if (source == null || target == null) {
            return target;
        }
        try {
            // 将两个对象都转为Map
            Map<String, Object> sourceMap = OBJECT_MAPPER.convertValue(source, 
                    new TypeReference<Map<String, Object>>() {});
            Map<String, Object> targetMap = OBJECT_MAPPER.convertValue(target, 
                    new TypeReference<Map<String, Object>>() {});
            
            // 将source的非空属性覆盖到target
            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                if (entry.getValue() != null) {
                    targetMap.put(entry.getKey(), entry.getValue());
                }
            }
            
            // 将合并后的Map转回对象
            return OBJECT_MAPPER.convertValue(targetMap, (Class<T>) target.getClass());
        } catch (Exception e) {
            log.error("更新对象属性失败", e);
            throw new RuntimeException("更新对象属性失败", e);
        }
    }

    /**
     * 将对象转换为Map
     *
     * @param obj 待转换的对象
     * @return Map对象
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 将Map转换为对象
     *
     * @param map   Map对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象
     */
    public static <T> T fromMap(Map<?, ?> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * 将 Map 转换为 Gson JsonObject，用于 Milvus JSON 字段
     */
    public static JsonObject mapToJsonObject(Map<String, Object> map) {
        JsonObject jsonObject = new JsonObject();
        if (map == null) {
            return jsonObject;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                jsonObject.add(key, null);
            } else if (value instanceof String) {
                jsonObject.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                jsonObject.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) value);
            } else {
                jsonObject.addProperty(key, value.toString());
            }
        }
        return jsonObject;
    }
}

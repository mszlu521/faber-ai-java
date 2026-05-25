package com.mszlu.ai.mcp.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础工具服务
 * 提供时间、计算器等基础功能
 */
@Slf4j
@Service
public class BasicTools {

    /**
     * 获取当前服务器时间
     *
     * @param format 时间格式，可选，默认为 yyyy-MM-dd HH:mm:ss
     * @return 当前时间字符串
     */
    @Tool(description = "获取当前服务器时间")
    public String getCurrentTime(String format) {
        log.info("Getting current time with format: {}", format);
        
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.now().format(formatter);
    }

    /**
     * 简单的加法计算
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 两数之和
     */
    @Tool(description = "执行加法运算，返回两个数字的和")
    public double add(double a, double b) {
        log.info("Calculating: {} + {}", a, b);
        return a + b;
    }

    /**
     * 简单的减法计算
     *
     * @param a 被减数
     * @param b 减数
     * @return 两数之差
     */
    @Tool(description = "执行减法运算，返回两个数字的差")
    public double subtract(double a, double b) {
        log.info("Calculating: {} - {}", a, b);
        return a - b;
    }

    /**
     * 简单的乘法计算
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 两数之积
     */
    @Tool(description = "执行乘法运算，返回两个数字的积")
    public double multiply(double a, double b) {
        log.info("Calculating: {} * {}", a, b);
        return a * b;
    }

    /**
     * 简单的除法计算
     *
     * @param a 被除数
     * @param b 除数
     * @return 两数之商
     */
    @Tool(description = "执行除法运算，返回两个数字的商")
    public double divide(double a, double b) {
        log.info("Calculating: {} / {}", a, b);
        
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        
        return a / b;
    }

    /**
     * 获取服务器信息
     *
     * @return 服务器信息 Map
     */
    @Tool(description = "获取当前服务器的基本信息")
    public Map<String, Object> getServerInfo() {
        log.info("Getting server info");
        
        Map<String, Object> info = new HashMap<>();
        info.put("serverName", "Faber MCP Server");
        info.put("version", "1.0.0");
        info.put("currentTime", LocalDateTime.now().toString());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        
        return info;
    }

    /**
     * 字符串转大写
     *
     * @param text 输入文本
     * @return 大写文本
     */
    @Tool(description = "将输入文本转换为大写")
    public String toUpperCase(String text) {
        log.info("Converting to uppercase: {}", text);
        
        if (text == null) {
            return null;
        }
        
        return text.toUpperCase();
    }

    /**
     * 字符串转小写
     *
     * @param text 输入文本
     * @return 小写文本
     */
    @Tool(description = "将输入文本转换为小写")
    public String toLowerCase(String text) {
        log.info("Converting to lowercase: {}", text);
        
        if (text == null) {
            return null;
        }
        
        return text.toLowerCase();
    }

    /**
     * 反转字符串
     *
     * @param text 输入文本
     * @return 反转后的文本
     */
    @Tool(description = "反转输入字符串")
    public String reverseString(String text) {
        log.info("Reversing string: {}", text);
        
        if (text == null) {
            return null;
        }
        
        return new StringBuilder(text).reverse().toString();
    }
}

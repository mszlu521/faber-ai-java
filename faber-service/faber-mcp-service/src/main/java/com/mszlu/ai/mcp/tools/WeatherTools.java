package com.mszlu.ai.mcp.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 天气工具服务
 * 提供模拟的天气查询功能
 */
@Slf4j
@Service
public class WeatherTools {

    private final Random random = new Random();

    /**
     * 查询指定城市的天气
     *
     * @param city 城市名称
     * @return 天气信息
     */
    @Tool(description = "查询指定城市的当前天气情况")
    public Map<String, Object> getWeather(String city) {
        log.info("Querying weather for city: {}", city);
        
        if (city == null || city.isEmpty()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        
        Map<String, Object> weather = new HashMap<>();
        weather.put("city", city);
        weather.put("temperature", 15 + random.nextInt(20)); // 15-35度
        weather.put("humidity", 40 + random.nextInt(40)); // 40-80%
        weather.put("condition", getRandomWeatherCondition());
        weather.put("windSpeed", 5 + random.nextInt(20)); // 5-25 km/h
        
        return weather;
    }

    /**
     * 获取未来几天的天气预报
     *
     * @param city 城市名称
     * @param days 天数，1-7
     * @return 天气预报列表
     */
    @Tool(description = "获取指定城市未来几天的天气预报")
    public Map<String, Object>[] getWeatherForecast(String city, int days) {
        log.info("Querying weather forecast for city: {}, days: {}", city, days);
        
        if (city == null || city.isEmpty()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        
        if (days < 1 || days > 7) {
            throw new IllegalArgumentException("天数必须在1-7之间");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object>[] forecasts = new Map[days];
        
        for (int i = 0; i < days; i++) {
            Map<String, Object> forecast = new HashMap<>();
            forecast.put("day", i + 1);
            forecast.put("city", city);
            forecast.put("highTemp", 20 + random.nextInt(15)); // 20-35度
            forecast.put("lowTemp", 10 + random.nextInt(10)); // 10-20度
            forecast.put("condition", getRandomWeatherCondition());
            forecast.put("precipitation", random.nextInt(100)); // 0-100%
            
            forecasts[i] = forecast;
        }
        
        return forecasts;
    }

    /**
     * 随机获取天气状况
     */
    private String getRandomWeatherCondition() {
        String[] conditions = {"晴", "多云", "阴", "小雨", "中雨", "大雨", "雷阵雨", "雪"};
        return conditions[random.nextInt(conditions.length)];
    }
}

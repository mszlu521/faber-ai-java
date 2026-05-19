package com.mszlu.ai.core.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${amap.api.key}")
    private String amapApiKey;

    private static final String AMAP_WEATHER_API_URL = "https://restapi.amap.com/v3/weather/weatherInfo";
    @Tool(name = "get_weather", description = "查询指定城市的天气信息，使用高德天气API")
    public String getCurrentWeather(
            @ToolParam(description = "需要查询天气的城市名称") String city,
            @ToolParam(description = "气象类型：base(实况天气)/all(预报天气)") String extensions
    ) {
        try {
            if (amapApiKey == null || amapApiKey.isEmpty()){
                return "请先配置高德地图API密钥";
            }
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_WEATHER_API_URL)
                    .queryParam("key", amapApiKey)
                    .queryParam("city", city)
                    .queryParam("extensions", extensions)
                    .queryParam("output", "json")
                    .build().toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode() != HttpStatus.OK){
                return "查询天气失败: " + response.getStatusCode();
            }
            return response.getBody();
        }catch (Exception e){
            log.error("查询天气异常", e);
            return "查询天气异常: " + e.getMessage();
        }
    }
}

package com.mszlu.ai.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class TestController {

    @RequestMapping("/test")
    public Map<String,Object> test(){
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello from Auth Service!");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}

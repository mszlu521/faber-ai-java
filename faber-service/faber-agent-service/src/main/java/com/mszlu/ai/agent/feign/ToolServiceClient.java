package com.mszlu.ai.agent.feign;

import com.mszlu.ai.agent.dto.ToolDTO;
import com.mszlu.ai.common.result.Result;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "faber-tool-service", path = "/api/v1/tools")
public interface ToolServiceClient {

    @GetMapping("/{id}")
    Result<ToolDTO> getTool(@PathVariable UUID id);
}

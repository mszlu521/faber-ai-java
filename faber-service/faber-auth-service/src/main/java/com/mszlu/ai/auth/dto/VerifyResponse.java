package com.mszlu.ai.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyResponse {
    private String token;
    private String message;
}

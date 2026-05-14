package com.mszlu.ai.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private Long expire;
    private String token;
    private String refreshToken;
    private Long refreshExpire;
    private UserInfo userInfo;

    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private String avatar;
    }
}

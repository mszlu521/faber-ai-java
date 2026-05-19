package com.mszlu.ai.gateway.filter;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.common.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.mszlu.ai.common.constants.CommonConstants.HEADER_USERNAME;
import static com.mszlu.ai.common.constants.CommonConstants.HEADER_USER_ID;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AuthProperties authProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) ->  {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            //检查白名单
            if (isWhitelisted(path)){
                return chain.filter(exchange);
            }
            //获取token
            String token = extractToken(request);
            if (token == null){
                log.error("Token is missing");
                String message = "Token is missing";
                return unauthorized(exchange.getResponse(),message);
            }
            try {
                //验证token
                if (!jwtUtils.validateToken(token)) {
                    return unauthorized(exchange.getResponse(), "Invalid Token");
                }
                //提取用户信息并传递到下游服务
                String userId = jwtUtils.getUserIdFromToken(token);
                String username = jwtUtils.getUsernameFromToken(token);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(HEADER_USER_ID, userId)
                        .header(HEADER_USERNAME, username)
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }catch (Exception e){
                return unauthorized(exchange.getResponse(), "Invalid Token");
            }
        };
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.error(HttpStatus.UNAUTHORIZED.value(), message);
        String body = toJson(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String toJson(Result<Void> result) {
        return String.format("{\"code\":%d,\"msg\":\"%s\"}", result.getCode(), result.getMsg());
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")){
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String path) {
        List<String> whitelist = authProperties.getWhitelist();
        if (whitelist == null){
            return false;
        }
        return whitelist.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Data
    public static class Config {
    }
}

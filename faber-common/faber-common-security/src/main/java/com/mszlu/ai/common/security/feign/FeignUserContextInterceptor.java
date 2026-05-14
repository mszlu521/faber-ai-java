package com.mszlu.ai.common.security.feign;

import com.mszlu.ai.common.security.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.mszlu.ai.common.constants.CommonConstants.HEADER_USER_ID;

@Slf4j
public class FeignUserContextInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        //获取当前线程id
        log.info("feign request thread id:{}",Thread.currentThread().getId());
        UUID userId = UserContext.getUserId();
        if (userId != null){
            requestTemplate.header(HEADER_USER_ID,userId.toString());
        }
    }
}

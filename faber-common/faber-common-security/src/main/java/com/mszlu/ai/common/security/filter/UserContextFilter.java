package com.mszlu.ai.common.security.filter;

import com.mszlu.ai.common.security.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.mszlu.ai.common.constants.CommonConstants.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class UserContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String userId = request.getHeader(HEADER_USER_ID);
            String username = request.getHeader(HEADER_USERNAME);
            String role = request.getHeader(HEADER_USER_ROLE);
            if (userId != null){
                UserContext context = UserContext.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .build();
                UserContext.set(context);
            }
            filterChain.doFilter(servletRequest,servletResponse);
        } finally {
            UserContext.clear();
        }
    }
}

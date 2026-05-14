package com.mszlu.ai.auth.service;

import com.mszlu.ai.auth.entity.User;
import com.mszlu.ai.auth.mapper.UserMapper;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    @Value("${spring.mail.username}")
    private String from;
    @Value("${app.base-url}")
    private String baseUrl;
    public void sendVerificationEmail(String email, String username, String verifyToken) {
        String verifyUrl = baseUrl + "/api/v1/auth/verify?token=" + verifyToken;
        String subject = "邮箱验证";
        String body = String.format(
                """
                <html>
                <body>
                    <p>您好，%s</p>
                    <p>请点击以下链接验证您的邮箱：</p>
                    <a href="%s">%s</a>
                </body>
                </html>
                """, username, verifyUrl, "点击验证邮箱"
        );
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("发送验证邮件成功: {}", email);
    }

    public void verifyEmail(String token, HttpServletResponse response) {
        String key = "verify_token:" + token;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null || userId.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(),"无效的token");
        }
        //删除令牌
        redisTemplate.delete(key);
        //查询用户
        UUID id = UUID.fromString(userId);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),"用户不存在");
        }
        //如果邮箱已经验证直接返回
        if (user.getEmailVerified()) {
            return;
        }
        user.setEmailVerified(true);
        user.setStatus(User.STATUS_ENABLED);
        userMapper.updateById(user);
    }

    public void sendForgotPasswordEmail(String email, String username, String code) {
        String subject = "忘记密码-验证码";
        String body = String.format(
                """
                尊敬的用户 %s,
                您正在重置密码，验证码是%s,
                验证码五分钟内有效，如非本人操作请忽略!!
                """, username, code
        );
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

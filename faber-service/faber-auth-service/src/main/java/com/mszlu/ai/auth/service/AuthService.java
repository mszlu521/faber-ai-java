package com.mszlu.ai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mszlu.ai.auth.dto.*;
import com.mszlu.ai.auth.entity.User;
import com.mszlu.ai.auth.mapper.UserMapper;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.result.ResultCode;
import com.mszlu.ai.common.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        //先检查用户名是否存在
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(1001, "用户名已存在");
        }
        //检查邮箱是否已经存在
        queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, request.getEmail());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(1002, "邮箱已存在");
        }
        //加密密码
        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        //生成验证令牌 这个是用于邮件验证的
        String verifyToken = generateVerifyToken();

        //创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encryptedPassword);
        user.setEmail(request.getEmail());
        user.setStatus(User.STATUS_PENDING);
        user.setEmailVerified( false);
        user.setCurrentPlan(User.PLAN_FREE);
        userMapper.insert(user);
        //接下来就是发邮件
        //先存储验证令牌到redis
        String key = "verify_token:" + verifyToken;
        redisTemplate.opsForValue().set(key, user.getId().toString(), 24, TimeUnit.HOURS);
        //发送邮件
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verifyToken);
        return RegisterResponse.builder().message("注册成功，请前往邮箱验证").build();
    }

    private String generateVerifyToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString( bytes);
    }

    @Transactional
    public LoginResponse login(@Valid LoginRequest request) {
        //查找用户 支持用户名和邮箱
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUsername, request.getUsername());
        queryWrapper.or().eq(User::getEmail, request.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(1003, "用户不存在");
        }
        //检查用户状态
        if (!Objects.equals(user.getStatus(), User.STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        //验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        //更新最后登录时间
        user.setLastLoginTime(OffsetDateTime.now());
        userMapper.updateById(user);
        return generateTokenResponse(user);
    }

    private LoginResponse generateTokenResponse(User user) {
        String token = jwtUtils.generateToken(user.getId().toString(),user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId().toString(),user.getUsername());
        Date tokenExpire = jwtUtils.getExpirationDateFromToken(token);
        Date refreshExpire = jwtUtils.getExpirationDateFromToken(refreshToken);
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id( user.getId().toString())
                .username( user.getUsername())
                .email( user.getEmail())
                .avatar( user.getAvatar())
                .build();
        return LoginResponse.builder()
                .token( token)
                .refreshToken( refreshToken)
                .expire( tokenExpire.getTime())
                .refreshExpire( refreshExpire.getTime())
                .userInfo(userInfo)
                .build();
    }

    public LoginResponse refreshToken(String refreshToken) {
        Claims claims = jwtUtils.parseToken(refreshToken);
        String userId = claims.get("userId", String.class);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return generateTokenResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, request.getEmail());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        //生成6位验证码
        String code = generate6DigitCode();
        //存储到redis中
        String key = "forgot_password_code:" + request.getEmail();
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        //发送邮件
        emailService.sendForgotPasswordEmail(user.getEmail(), user.getUsername(), code);
    }

    private String generate6DigitCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public VerifyResponse verifyCode(VerifyCodeRequest request) {
        //从redis获取验证码
        String key = "forgot_password_code:" + request.getEmail();
        String code = redisTemplate.opsForValue().get(key);
        if (!Objects.equals(code, request.getCode())) {
            throw new BusinessException(ResultCode.VERIFY_CODE_ERROR);
        }
        redisTemplate.delete(key);
        //生成重置的token
        String token = generateResetToken();
        String tokenKey = "reset_password_token:" + token;
        redisTemplate.opsForValue().set(tokenKey, request.getEmail(), 10, TimeUnit.MINUTES);
        return VerifyResponse.builder()
                .token(token)
                .message("验证成功")
                .build();
    }

    private String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString( bytes);
    }

    @Transactional
    public void resetPassword(@Valid ResetPasswordRequest request) {
        //验证token
        String key = "reset_password_token:" + request.getToken();
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        redisTemplate.delete(key);
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}

package com.mszlu.ai.auth.controller;

import com.mszlu.ai.auth.dto.*;
import com.mszlu.ai.auth.service.AuthService;
import com.mszlu.ai.auth.service.EmailService;
import com.mszlu.ai.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/refresh-token")
    public Result<LoginResponse> refreshToken(@RequestBody Map<String,String> request) {
        String refreshToken = request.get("refreshToken");
        LoginResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest  request) {
        authService.forgotPassword(request);
        return Result.success();
    }
    @PostMapping("/verify-code")
    public Result<VerifyResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest  request) {
        VerifyResponse response = authService.verifyCode(request);
        return Result.success(response);
    }
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest  request) {
        authService.resetPassword(request);
        return Result.success();
    }
    @GetMapping("/verify")
    public void verifyEmail(@RequestParam String token, HttpServletResponse response) throws IOException {
        emailService.verifyEmail(token, response);
        response.sendRedirect("http://localhost:5173/login");
    }
}

package com.mszlu.ai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email")
    private String email;
    @NotBlank(message = "Token cannot be blank")
    private String token;
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
}

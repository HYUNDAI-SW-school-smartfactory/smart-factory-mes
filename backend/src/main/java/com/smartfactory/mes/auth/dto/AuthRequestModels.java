package com.smartfactory.mes.auth.dto;

public final class AuthRequestModels {

    private AuthRequestModels() {
    }

    public record LoginRequest(
            String email,
            String password
    ) {
    }

    public record SignUpRequest(
            String username,
            String displayName,
            String email,
            String password,
            String confirmPassword
    ) {
    }
}

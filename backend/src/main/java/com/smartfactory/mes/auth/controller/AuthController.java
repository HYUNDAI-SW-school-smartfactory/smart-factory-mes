package com.smartfactory.mes.auth.controller;

import com.smartfactory.mes.auth.dto.AuthRequestModels;
import com.smartfactory.mes.auth.dto.AuthResponseModels;
import com.smartfactory.mes.auth.service.AuthService;
import com.smartfactory.mes.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<AuthResponseModels.AuthSessionResponse> signUp(
            @RequestBody AuthRequestModels.SignUpRequest request
    ) {
        return ApiResponse.success("Sign up completed.", authService.signUp(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponseModels.AuthSessionResponse> login(
            @RequestBody AuthRequestModels.LoginRequest request
    ) {
        return ApiResponse.success("Login completed.", authService.login(request));
    }
}

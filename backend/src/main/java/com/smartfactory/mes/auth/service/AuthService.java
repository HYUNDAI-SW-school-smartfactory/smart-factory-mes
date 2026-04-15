package com.smartfactory.mes.auth.service;

import com.smartfactory.mes.auth.domain.AppUser;
import com.smartfactory.mes.auth.dto.AuthRequestModels;
import com.smartfactory.mes.auth.dto.AuthResponseModels;
import com.smartfactory.mes.auth.persistence.mapper.AuthUserMapper;
import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserMapper authUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponseModels.AuthSessionResponse signUp(AuthRequestModels.SignUpRequest request) {
        String username = normalize(request.username());
        String displayName = normalize(request.displayName());
        String email = normalizeEmail(request.email());
        String password = request.password();
        String confirmPassword = request.confirmPassword();

        validateSignUp(username, displayName, email, password, confirmPassword);

        if (authUserMapper.selectByUsername(username) != null || authUserMapper.selectByEmail(email) != null) {
            throw new BusinessException(ErrorCode.AUTH_USER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        AppUser user = AppUser.builder()
                .username(username)
                .displayName(displayName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(now)
                .updatedAt(now)
                .build();

        authUserMapper.insert(user);
        return buildSessionResponse(user);
    }

    public AuthResponseModels.AuthSessionResponse login(AuthRequestModels.LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password();

        if (email.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Email and password are required.");
        }

        AppUser user = authUserMapper.selectByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        return buildSessionResponse(user);
    }

    private void validateSignUp(
            String username,
            String displayName,
            String email,
            String password,
            String confirmPassword
    ) {
        if (username.length() < 4 || username.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Username must be between 4 and 20 characters.");
        }

        if (!username.matches("[a-zA-Z0-9._-]+")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Username may contain letters, numbers, dot, underscore, and dash only.");
        }

        if (displayName.length() < 2 || displayName.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Display name must be between 2 and 30 characters.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Email format is invalid.");
        }

        if (password == null || password.length() < 8) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Password must be at least 8 characters.");
        }

        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Password confirmation does not match.");
        }
    }

    private AuthResponseModels.AuthSessionResponse buildSessionResponse(AppUser user) {
        String token = "mes_" + UUID.randomUUID().toString().replace("-", "");
        return new AuthResponseModels.AuthSessionResponse(
                token,
                new AuthResponseModels.AuthUserResponse(
                        user.getUserId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail()
                )
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }
}

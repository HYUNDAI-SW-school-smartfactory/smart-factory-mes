package com.smartfactory.mes.auth.dto;

public final class AuthResponseModels {

    private AuthResponseModels() {
    }

    public record AuthUserResponse(
            Long userId,
            String username,
            String displayName,
            String email
    ) {
    }

    public record AuthSessionResponse(
            String token,
            AuthUserResponse user
    ) {
    }
}

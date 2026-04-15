package com.smartfactory.mes.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    private Long userId;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

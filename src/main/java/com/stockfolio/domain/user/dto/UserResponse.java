package com.stockfolio.domain.user.dto;

import com.stockfolio.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final LocalDateTime createdAt;

    private UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt();
    }

    public static UserResponse from(User user) {
        return new UserResponse(user);
    }
}

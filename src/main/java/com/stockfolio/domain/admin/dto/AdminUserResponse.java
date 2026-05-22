package com.stockfolio.domain.admin.dto;

import com.stockfolio.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminUserResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    private AdminUserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole().name();
        this.isActive = user.isActive();
        this.createdAt = user.getCreatedAt();
        this.deletedAt = user.getDeletedAt();
    }

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user);
    }
}

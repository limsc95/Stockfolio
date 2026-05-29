package com.stockfolio.domain.user.entity;

import com.stockfolio.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        indexes = @Index(name = "uq_users_email", columnList = "email", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // NOTE: 필드명을 'active'로 지정해야 JPA 속성명(active)과 Bean 프로퍼티명(isActive() → active)이
    //       일치하여 Hibernate 6의 JPQL 타입 체크를 통과한다. DB 컬럼명은 is_active 로 유지한다.
    @Column(nullable = false, name = "is_active")
    private boolean active;

    @Column
    private LocalDateTime deletedAt;

    // ── 생성자 ────────────────────────────────────────────
    @Builder
    private User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.active = true;
    }

    // ── 도메인 메서드 ──────────────────────────────────────
    public void updateName(String name) {
        this.name = name;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void deactivate() {
        this.active = false;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.active = false;
    }

    // ── Role Enum ────────────────────────────────────────
    public enum Role {
        USER, ADMIN
    }
}

package com.stockfolio.domain.user.service;

import com.stockfolio.domain.user.dto.ChangePasswordRequest;
import com.stockfolio.domain.user.dto.UpdateProfileRequest;
import com.stockfolio.domain.user.dto.UserResponse;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import com.stockfolio.infra.redis.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    // ── 내 정보 조회 ──────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findActiveUser(userId);
        return UserResponse.from(user);
    }

    // ── 프로필 수정 ───────────────────────────────────────
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.updateName(request.getName());
        return UserResponse.from(user);
    }

    // ── 비밀번호 변경 ─────────────────────────────────────
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        // 비밀번호 변경 시 모든 세션 무효화 (Redis에서 Refresh Token 삭제)
        refreshTokenStore.delete(userId);
        log.info("[User] 비밀번호 변경 완료 - userId={}", userId);
    }

    // ── 회원 탈퇴 (Soft Delete) ───────────────────────────
    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);
        user.softDelete();
        refreshTokenStore.delete(userId);
        log.info("[User] 회원 탈퇴 완료 - userId={}", userId);
    }

    // ── 내부 공통 조회 ────────────────────────────────────
    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

package com.stockfolio.domain.user.service;

import com.stockfolio.domain.user.dto.*;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import com.stockfolio.global.security.JwtProvider;
import com.stockfolio.infra.redis.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    // ── 회원가입 ─────────────────────────────────────────
    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[Auth] 회원가입 완료 - userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    // ── 로그인 ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.DEACTIVATED_USER);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return issueTokens(user);
    }

    // ── Access Token 재발급 ──────────────────────────────
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtProvider.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = Long.parseLong(claims.getSubject());

        // Redis에 저장된 토큰과 일치 여부 확인
        String storedToken = refreshTokenStore.find(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (!storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    // ── 로그아웃 ─────────────────────────────────────────
    public void logout(Long userId, String accessToken) {
        refreshTokenStore.delete(userId);
        refreshTokenStore.blacklistAccessToken(accessToken);
        log.info("[Auth] 로그아웃 완료 - userId={}", userId);
    }

    // ── 토큰 발급 (공통) ──────────────────────────────────
    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        refreshTokenStore.save(user.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken, accessTokenExpiry);
    }
}

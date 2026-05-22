package com.stockfolio.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 Refresh Token 저장소
 *
 * Key 구조:
 *   refresh_token:{userId}  →  refreshToken 문자열  (TTL: refresh-token-expiry)
 *   logout_blacklist:{token} →  "logout"            (TTL: access-token-expiry)
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_KEY_PREFIX   = "refresh_token:";
    private static final String BLACKLIST_KEY_PREFIX  = "logout_blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    // ── Refresh Token 저장 ───────────────────────────────
    public void save(Long userId, String refreshToken) {
        String key = REFRESH_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(refreshTokenExpiry));
    }

    // ── Refresh Token 조회 ───────────────────────────────
    public Optional<String> find(Long userId) {
        String key = REFRESH_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value).map(Object::toString);
    }

    // ── Refresh Token 삭제 (로그아웃) ────────────────────
    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    // ── Access Token 블랙리스트 등록 (로그아웃 시) ────────
    public void blacklistAccessToken(String accessToken) {
        String key = BLACKLIST_KEY_PREFIX + accessToken;
        redisTemplate.opsForValue().set(key, "logout", Duration.ofSeconds(accessTokenExpiry));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + accessToken));
    }
}

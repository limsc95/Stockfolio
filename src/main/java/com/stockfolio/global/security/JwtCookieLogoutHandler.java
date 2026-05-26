package com.stockfolio.global.security;

import com.stockfolio.infra.redis.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieLogoutHandler implements LogoutHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) return;

        try {
            if (jwtProvider.validateToken(token)) {
                Long userId = jwtProvider.getUserId(token);
                refreshTokenStore.delete(userId);
                refreshTokenStore.blacklistAccessToken(token);
                log.info("[Logout] userId={} 로그아웃 처리 완료", userId);
            }
        } catch (Exception e) {
            log.warn("[Logout] 토큰 무효화 중 오류 (무시): {}", e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("SF_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

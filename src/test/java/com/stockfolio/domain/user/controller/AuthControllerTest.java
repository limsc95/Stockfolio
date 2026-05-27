package com.stockfolio.domain.user.controller;

import com.stockfolio.domain.user.dto.TokenResponse;
import com.stockfolio.domain.user.dto.UserResponse;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.service.AuthService;
import com.stockfolio.global.config.JpaAuditingConfig;
import com.stockfolio.global.config.RabbitMQConfig;
import com.stockfolio.global.config.RedisConfig;
import com.stockfolio.global.config.WebClientConfig;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import com.stockfolio.global.security.CustomUserDetailsService;
import com.stockfolio.global.security.JwtCookieLogoutHandler;
import com.stockfolio.global.security.JwtProvider;
import com.stockfolio.infra.redis.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합(슬라이스) 테스트
 *
 * @WebMvcTest(AuthController.class):
 *   - DispatcherServlet, Spring MVC, Spring Security, @ControllerAdvice 등 웹 계층만 로드한다.
 *   - @Configuration 클래스도 기본적으로 로드되므로 Redis·RabbitMQ Config를 명시적으로 제외한다.
 *     (인프라 Auto-Configuration 비활성화는 src/test/resources/application.yml 로 처리)
 *   - SecurityConfig가 JwtProvider, CustomUserDetailsService, RefreshTokenStore,
 *     JwtCookieLogoutHandler 에 의존하므로 4개 모두 @MockBean 으로 제공해야 한다.
 *
 * 검증 내용:
 *   - HTTP 상태 코드
 *   - ApiResponse 래퍼 (success 필드, data / error 구조)
 *   - @Valid 검증 실패 시 GlobalExceptionHandler가 400을 반환하는지
 *   - BusinessException → GlobalExceptionHandler → 적절한 HTTP 상태 + error.code 매핑
 */
@WebMvcTest(
        value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {RedisConfig.class, RabbitMQConfig.class, WebClientConfig.class, JpaAuditingConfig.class}
        )
)
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    // ── 웹 계층 보안 의존성 (SecurityConfig가 요구) ──────────
    @MockBean AuthService              authService;
    @MockBean JwtProvider              jwtProvider;
    @MockBean CustomUserDetailsService userDetailsService;
    @MockBean RefreshTokenStore        refreshTokenStore;
    @MockBean JwtCookieLogoutHandler   jwtCookieLogoutHandler;

    // ── 픽스처 헬퍼 ────────────────────────────────────────
    /**
     * UserResponse는 private 생성자 + static factory 만 지원하므로
     * ReflectionTestUtils로 id/createdAt 을 주입한다.
     */
    private UserResponse sampleUserResponse() {
        User user = User.builder()
                .email("new@test.com").password("encoded").name("홍길동").role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return UserResponse.from(user);
    }

    private TokenResponse sampleTokenResponse() {
        return new TokenResponse("access_token_value", "refresh_token_value", 3600L);
    }

    // ════════════════════════════════════════════════════
    // POST /api/v1/auth/signup
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/auth/signup (회원가입)")
    class SignUpTest {

        @Test
        @DisplayName("정상 요청 시 201 Created, success=true, data.email 반환")
        void signup_success_returns_201() throws Exception {
            given(authService.signUp(any())).willReturn(sampleUserResponse());

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@test.com",
                                        "password": "password123!",
                                        "name": "홍길동"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("new@test.com"))
                    .andExpect(jsonPath("$.data.name").value("홍길동"));
        }

        @Test
        @DisplayName("이메일 형식 오류 → @Valid 실패 → 400 Bad Request")
        void signup_invalidEmail_returns_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "not-an-email",
                                        "password": "password123!",
                                        "name": "홍길동"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("COMMON_001"));
        }

        @Test
        @DisplayName("비밀번호 8자 미만 → @Valid 실패 → 400 Bad Request")
        void signup_shortPassword_returns_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@test.com",
                                        "password": "short",
                                        "name": "홍길동"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("이름 누락 → @Valid 실패 → 400 Bad Request")
        void signup_missingName_returns_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "new@test.com",
                                        "password": "password123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("중복 이메일 → EMAIL_ALREADY_EXISTS → 409 Conflict")
        void signup_duplicateEmail_returns_409() throws Exception {
            given(authService.signUp(any()))
                    .willThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "dup@test.com",
                                        "password": "password123!",
                                        "name": "홍길동"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("USER_002"));
        }
    }

    // ════════════════════════════════════════════════════
    // POST /api/v1/auth/login
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/auth/login (로그인)")
    class LoginTest {

        @Test
        @DisplayName("올바른 자격증명 → 200 OK, accessToken / refreshToken / expiresIn 반환")
        void login_success_returns_token() throws Exception {
            given(authService.login(any())).willReturn(sampleTokenResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "user@test.com",
                                        "password": "password123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access_token_value"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh_token_value"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → USER_NOT_FOUND → 404 Not Found")
        void login_unknownEmail_returns_404() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "ghost@test.com",
                                        "password": "password123!"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("USER_001"));
        }

        @Test
        @DisplayName("비밀번호 불일치 → INVALID_PASSWORD → 400 Bad Request")
        void login_wrongPassword_returns_400() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "user@test.com",
                                        "password": "wrongPw123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("USER_003"));
        }

        @Test
        @DisplayName("비활성화된 계정 → DEACTIVATED_USER → 403 Forbidden")
        void login_deactivatedUser_returns_403() throws Exception {
            given(authService.login(any()))
                    .willThrow(new BusinessException(ErrorCode.DEACTIVATED_USER));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "user@test.com",
                                        "password": "password123!"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("USER_004"));
        }

        @Test
        @DisplayName("이메일 누락 → @Valid 실패 → 400 Bad Request")
        void login_missingEmail_returns_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "password": "password123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}

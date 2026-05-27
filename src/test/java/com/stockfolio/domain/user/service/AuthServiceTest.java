package com.stockfolio.domain.user.service;

import com.stockfolio.domain.user.dto.*;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import com.stockfolio.global.security.JwtProvider;
import com.stockfolio.infra.redis.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 단위 테스트
 *
 * 모든 의존성을 Mock으로 대체하여 서비스 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock UserRepository    userRepository;
    @Mock PasswordEncoder   passwordEncoder;
    @Mock JwtProvider       jwtProvider;
    @Mock RefreshTokenStore refreshTokenStore;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        // @Value 필드는 ReflectionTestUtils로 직접 주입
        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 3600L);
    }

    // ── 테스트용 User 생성 헬퍼 ───────────────────────────
    private User activeUser(Long id, String email) {
        User user = User.builder()
                .email(email)
                .password("encoded_password")
                .name("테스터")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    // ════════════════════════════════════════════════════
    // signUp()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("회원가입(signUp)")
    class SignUpTest {

        @Test
        @DisplayName("정상 요청 시 UserResponse를 반환해야 한다")
        void signUp_success_returnsUserResponse() {
            // given
            SignUpRequest request = new SignUpRequest("new@test.com", "password123!", "홍길동");
            User savedUser = activeUser(1L, "new@test.com");

            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserResponse result = authService.signUp(request);

            // then
            assertThat(result.getEmail()).isEqualTo("new@test.com");
            assertThat(result.getName()).isEqualTo("테스터");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 EMAIL_ALREADY_EXISTS 예외가 발생해야 한다")
        void signUp_duplicateEmail_throwsBusinessException() {
            // given
            SignUpRequest request = new SignUpRequest("dup@test.com", "password123!", "홍길동");
            given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

            then(userRepository).should(never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════
    // login()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("로그인(login)")
    class LoginTest {

        @Test
        @DisplayName("올바른 자격증명으로 로그인 시 TokenResponse를 반환해야 한다")
        void login_validCredentials_returnsTokenResponse() {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "password");
            User user = activeUser(1L, "user@test.com");

            given(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches("password", "encoded_password")).willReturn(true);
            given(jwtProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                    .willReturn("access_token");
            given(jwtProvider.generateRefreshToken(anyLong()))
                    .willReturn("refresh_token");

            // when
            TokenResponse result = authService.login(request);

            // then
            assertThat(result.getAccessToken()).isEqualTo("access_token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(result.getExpiresIn()).isEqualTo(3600L);
            then(refreshTokenStore).should().save(eq(1L), eq("refresh_token"));
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 USER_NOT_FOUND 예외가 발생해야 한다")
        void login_unknownEmail_throwsUserNotFound() {
            // given
            LoginRequest request = new LoginRequest("ghost@test.com", "password");
            given(userRepository.findByEmailAndDeletedAtIsNull("ghost@test.com"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생해야 한다")
        void login_wrongPassword_throwsInvalidPassword() {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "wrongPw");
            User user = activeUser(1L, "user@test.com");

            given(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPw", "encoded_password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("비활성화된 계정이면 DEACTIVATED_USER 예외가 발생해야 한다")
        void login_deactivatedUser_throwsDeactivatedUser() {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "password");
            User user = activeUser(1L, "user@test.com");
            ReflectionTestUtils.setField(user, "isActive", false);

            given(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                    .willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DEACTIVATED_USER);
        }
    }

    // ════════════════════════════════════════════════════
    // logout()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("로그아웃(logout)")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 시 RefreshToken 삭제 및 AccessToken 블랙리스트 등록이 호출되어야 한다")
        void logout_callsDeleteAndBlacklist() {
            // when
            authService.logout(1L, "access_token_value");

            // then
            then(refreshTokenStore).should().delete(1L);
            then(refreshTokenStore).should().blacklistAccessToken("access_token_value");
        }
    }
}

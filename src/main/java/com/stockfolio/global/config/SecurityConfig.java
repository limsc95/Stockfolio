package com.stockfolio.global.config;

import com.stockfolio.global.security.CustomUserDetailsService;
import com.stockfolio.global.security.JwtAuthenticationFilter;
import com.stockfolio.global.security.JwtProvider;
import com.stockfolio.infra.redis.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // @PreAuthorize 사용 가능
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenStore refreshTokenStore;

    // ── 인증 불필요 경로 ──────────────────────────────────
    private static final String[] PUBLIC_GET_URLS = {
            "/api/v1/stocks",
            "/actuator/health",
    };

    private static final String[] PUBLIC_POST_URLS = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
    };

    private static final String[] SWAGGER_URLS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
    };

    private static final String[] PUBLIC_URLS = {
            "/",            // → Swagger 리다이렉트
            "/error",       // Spring 기본 에러 페이지
            "/favicon.ico",
    };

    private static final String[] ADMIN_PAGE_URLS = {
            "/admin",
            "/admin/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_URLS).permitAll()
                        .requestMatchers(ADMIN_PAGE_URLS).hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, userDetailsService, refreshTokenStore),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
